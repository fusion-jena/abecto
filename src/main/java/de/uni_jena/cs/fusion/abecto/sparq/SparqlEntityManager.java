package de.uni_jena.cs.fusion.abecto.sparq;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.update.UpdateAction;

public class SparqlEntityManager {

	private final static Map<Class<? extends AbstractSparqlEntity>, SelectBuilder> SELECT_QUERY_CACHE = new HashMap<>();

	private SparqlEntityManager() {
		// prevent instantiation
	}

	public static <T extends AbstractSparqlEntity> void insert(Collection<T> resources, Model target)
			throws NoSuchElementException {

		T prototype = resources.stream().findAny().get();
		Field[] fields = prototype.getClass().getFields();

		// manage prefixes
		Prologue prologue = new Prologue();
		for (Namespace namespaceAnnotation : prototype.getClass().getAnnotationsByType(Namespace.class)) {
			prologue.setPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
		}

		UpdateBuilder update = new UpdateBuilder(prologue.getPrefixMapping());

		for (T resource : resources) {
			// get resource identifier or new blank node
			Node subject;
			if (resource.id == null) {
				subject = NodeFactory.createBlankNode();
			} else {
				subject = resource.id.asNode();
			}
			for (Field field : fields) {
				Node property = property(field, prologue);
				try {
					Object value = field.get(resource);
					if ((value instanceof Collection)) {
						for (Object valueElement : (Collection<?>) value) {
							addInsert(update, subject, property, valueElement);
						}
					} else if (value instanceof Optional) {
						if (((Optional<?>) value).isPresent()) {
							addInsert(update, subject, property, ((Optional<?>) value).get());
						}
					} else {
						addInsert(update, subject, property, value);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Failed to access member " + field.getName(), e);
				}
			}
		}
		UpdateAction.execute(update.buildRequest(), target);
	}

	private static void addInsert(UpdateBuilder update, Node subject, Node predicate, Object object) {
		// TODO handle object values (Resources)
		update.addInsert(new Triple(subject, predicate, update.makeNode(object)));
	}

	private static <T extends AbstractSparqlEntity> SelectBuilder selectQuery(Class<T> type) {
		return SELECT_QUERY_CACHE.computeIfAbsent(type, (t) -> {
			SelectBuilder select = new SelectBuilder();

			// add prefixes
			for (Namespace namespaceAnnotation : type.getAnnotationsByType(Namespace.class)) {
				select.addPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
			}

			Var idVar = AbstractQueryBuilder.makeVar("id");

			for (Field field : type.getFields()) {
				Path pathPattern = propertyPath(field, select.getPrologHandler().getPrefixes());
				Var fieldVar = AbstractQueryBuilder.makeVar(field.getName());
				TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, fieldVar);
				if (Optional.class.isAssignableFrom(field.getType())) {
					select.addOptional(triplePath);
				} else {
					// non optional
					select.addWhere(triplePath);
				}
			}

			return select;
		}).clone();
	}

	@SuppressWarnings("unchecked")
	public static <T extends AbstractSparqlEntity> Set<T> select(T prototype, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		// get plain query
		SelectBuilder select = selectQuery(prototype.getClass());
		// add prototype values to query
		Var idVar = AbstractQueryBuilder.makeVar("id");
		for (Field field : prototype.getClass().getFields()) {
			if (field.get(prototype) != null) {
				Path pathPattern = propertyPath(field, select.getPrologHandler().getPrefixes());
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection<Object> values = (Collection<Object>) field.get(prototype);
					for (Object value : values) {
						TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, select.makeNode(value));
						select.addWhere(triplePath);
					}
				} else if (Optional.class.isAssignableFrom(field.getType())) {
					Optional<Object> value = (Optional<Object>) field.get(prototype);
					if (value.isPresent()) {
						TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, select.makeNode(value));
						select.addWhere(triplePath);
					} else {
						TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, Node.ANY);
						select.addFilter(select.getExprFactory().notexists(new SelectBuilder().addWhere(triplePath)));
					}
				} else {
					Object value = field.get(prototype);
					TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, select.makeNode(value));
					select.addWhere(triplePath);
				}
			}
		}
		// execute query
		ResultSet queryResults = QueryExecutionFactory.create(select.build(), source).execSelect();
		// generate result set
		Map<Resource, T> entityMap = new HashMap<>();
		while (queryResults.hasNext()) {
			QuerySolution queryResult = queryResults.next();
			Resource id = queryResult.getResource("id");
			boolean firstVisit;
			T entity;
			if (entityMap.containsKey(id)) {
				entity = entityMap.get(id);
				firstVisit = false;
			} else {
				entity = (T) prototype.getClass().getDeclaredConstructor().newInstance();
				entityMap.put(id, entity);
				firstVisit = true;
			}
			for (Field field : prototype.getClass().getFields()) {
				RDFNode node = queryResult.get(field.getName());
				// TODO handle object values (Resources)
				if (Collection.class.isAssignableFrom(field.getType())) {
					((Collection<Object>) field.get(entity)).add(node.asLiteral().getValue());
				} else {
					Object newValue;
					if (Optional.class.isAssignableFrom(field.getType())) {
						if (node == null) {
							newValue = Optional.empty();
						} else {
							newValue = Optional.of(node.asLiteral().getValue());
						}
					} else {
						if (node == null) {
							throw new IllegalStateException(
									String.format("Missing value for non optional member %s. of entity %s.",
											field.getName(), entity.id));
						} else {
							newValue = node.asLiteral().getValue();
						}
					}
					Object currentValue = field.get(entity);
					if (firstVisit) {
						field.set(entity, newValue);
					} else if (!currentValue.equals(newValue)) {
						throw new IllegalStateException(
								String.format("Multiple values for functional field %s of entity %s: \"%s\", \"%s\".",
										field.getName(), entity.id, currentValue, newValue));
					}
				}
			}
		}

		return new HashSet<T>(entityMap.values());
	}

	private static Path propertyPath(Field field, PrefixMapping prefixMapping)
			throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = field.getAnnotation(PropertyPattern.class).value();
			return PathParser.parse(propertyAnnotation, prefixMapping);
		} catch (NullPointerException e) {
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					PropertyPattern.class.getSimpleName(), field.getName()));
		} catch (QueryException e) {
			throw new IllegalArgumentException(
					String.format("Illegal %s annotation for member %s: Expected property path.",
							PropertyPattern.class.getSimpleName(), field.getName()),
					e);
		}
	}

	private static Node property(Field field, Prologue prologue) throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = field.getAnnotation(PropertyPattern.class).value();
			Path path = PathParser.parse(propertyAnnotation, prologue);
			if (path instanceof P_Link) {
				return ((P_Link) path).getNode();
			} else {
				throw new IllegalArgumentException(
						String.format("Illegal %s annotation for member %s: Expected single property.",
								PropertyPattern.class.getSimpleName(), field.getName()));
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					PropertyPattern.class.getSimpleName(), field.getName()));
		}
	}
}
