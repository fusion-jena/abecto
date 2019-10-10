package de.uni_jena.cs.fusion.abecto.sparq;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
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

	private static void addInsert(UpdateBuilder update, Node subject, Node predicate, Object object) {
		update.addInsert(new Triple(subject, predicate, update.makeNode(object)));
	}

	private static Object fieldValue(RDFNode node, Field field) {
		if (node.isLiteral()) {
			return node.asLiteral().getValue();
		} else if (node.isResource()) {
			return node.asResource();
		} else {
			throw new IllegalStateException(String.format("Illegal value for member %s.", field.getName()));
		}
	}

	public static <T extends AbstractSparqlEntity> void insert(T resource, Model target) {
		insert(Collections.singleton(resource), target);
	}

	/**
	 * Inserts resources into a {@link Model} .
	 * 
	 * @param <T>       the type of the resources
	 * @param resources the resources to insert
	 * @param target    the {@link Model} to insert the resources into
	 */
	public static <T extends AbstractSparqlEntity> void insert(Collection<T> resources, Model target) {
		if (resources.isEmpty()) {
			return;
		}

		T prototype = resources.stream().findAny().get();
		Field[] fields = prototype.getClass().getFields();

		// manage prefixes
		Prologue prologue = new Prologue();
		for (Namespace namespaceAnnotation : prototype.getClass().getAnnotationsByType(Namespace.class)) {
			prologue.setPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
		}

		UpdateBuilder update = new UpdateBuilder(prologue.getPrefixMapping());

		for (T resource : resources) {

			// get subject
			Node subject;
			if (resource.id == null) {
				subject = NodeFactory.createBlankNode();
			} else {
				subject = resource.id.asNode();
			}

			for (Field field : fields) {

				// get predicate
				Node property = property(field, prologue);

				// get object
				try {
					Object object = field.get(resource);
					if (object == null) {
						throw new NullPointerException(String.format("Missing value for member %s.", field.getName()));
					} else if ((object instanceof Collection)) {
						for (Object objectElement : (Collection<?>) object) {
							if (objectElement != null) {
								addInsert(update, subject, property, objectElement);
							} else {
								throw new NullPointerException(String
										.format("Null element contained in member collection %s.", field.getName()));
							}
						}
					} else if (object instanceof Optional) {
						if (((Optional<?>) object).isPresent()) {
							addInsert(update, subject, property, ((Optional<?>) object).get());
						}
					} else {
						addInsert(update, subject, property, object);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Failed to access member " + field.getName(), e);
				}
			}
		}

		// execute update
		UpdateAction.execute(update.buildRequest(), target);
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

	public static <T extends AbstractSparqlEntity> Set<T> select(T prototype, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		return select(Collections.singleton(prototype), source);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AbstractSparqlEntity> Set<T> select(Collection<T> prototypes, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		if (prototypes.isEmpty()) {
			return Collections.emptySet();
		}

		T prototype = prototypes.stream().findAny().get();

		// get plain query
		SelectBuilder select = selectQuery(prototype.getClass());
		ExprFactory expression = select.getExprFactory();

		// TODO support multiple prototypes

		// add prototype values to query
		Var idVar = AbstractQueryBuilder.makeVar("id");
		if (prototype.id != null) {
			select.addFilter(expression.eq(idVar, prototype.id));
		}
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
						select.addFilter(expression.notexists(new SelectBuilder().addWhere(triplePath)));
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

			entity.id = id;
			for (Field field : prototype.getClass().getFields()) {
				RDFNode node = queryResult.get(field.getName());
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection<Object> collection = (Collection<Object>) field.get(entity);
					if (collection != null) {
						if (node != null) {
							collection.add(fieldValue(node, field));
						}
					} else {
						throw new NullPointerException(
								String.format("Member collection %s not initialized.", field.getName()));
					}
				} else {
					Object newValue;
					if (Optional.class.isAssignableFrom(field.getType())) {
						if (node != null) {
							newValue = Optional.of(fieldValue(node, field));
						} else {
							newValue = Optional.empty();
						}
					} else {
						if (node != null) {
							newValue = fieldValue(node, field);
						} else {
							throw new IllegalStateException(
									String.format("Missing value for non optional member %s of entity %s.",
											field.getName(), entity.id));
						}
					}
					if (firstVisit) {
						field.set(entity, newValue);
					} else if (!field.get(entity).equals(newValue)) {
						throw new IllegalStateException(
								String.format("Multiple values for functional field %s of entity %s: \"%s\", \"%s\".",
										field.getName(), entity.id, field.get(entity), newValue));
					}
				}
			}
		}

		return new HashSet<T>(entityMap.values());
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
				if (Collection.class.isAssignableFrom(field.getType())
						|| Optional.class.isAssignableFrom(field.getType())) {
					select.addOptional(triplePath);
				} else {
					// non optional
					select.addWhere(triplePath);
				}
			}

			return select;
		}).clone();
	}

	private SparqlEntityManager() {
		// prevent instantiation
	}
}
