package de.uni_jena.cs.fusion.abecto.sparq;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.update.UpdateAction;

/**
 * Provides methods to insert or select objects into or from a {@link Model} via
 * SPARQL.
 * 
 * The SPARQL queries will be build automatically using field types and
 * annotations with {@link SparqlNamespace} and {@link SparqlPattern}.
 */
public class SparqlEntityManager {

	private final static Map<Class<? extends AbstractSparqlEntity>, SelectBuilder> SELECT_QUERY_CACHE = new HashMap<>();

	private static void addInsert(UpdateBuilder update, Node subject, Node predicate, Object object) {
		update.addInsert(new Triple(subject, predicate, update.makeNode(object)));
	}

	private static <T extends AbstractSparqlEntity> Expr objectFilter(T filterEntity, SelectBuilder select)
			throws ReflectiveOperationException, ARQInternalErrorException, IllegalArgumentException,
			NullPointerException {
		Collection<Expr> expressions = new ArrayList<>();
		ExprFactory factory = select.getPrologHandler().getExprFactory();

		Var idVar = AbstractQueryBuilder.makeVar("id");
		if (filterEntity.id != null) {
			expressions.add(factory.eq(idVar, filterEntity.id));
		}
		for (Field field : filterEntity.getClass().getFields()) {
			if (field.get(filterEntity) != null) {
				Path pathPattern = propertyPath(field, select.getPrologHandler().getPrefixes());
				if (Collection.class.isAssignableFrom(field.getType())) {
					@SuppressWarnings("unchecked")
					Collection<Object> values = (Collection<Object>) field.get(filterEntity);
					for (Object value : values) {
						TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, select.makeNode(value));
						expressions.add(factory.exists(new SelectBuilder().addWhere(triplePath)));
					}
				} else if (Optional.class.isAssignableFrom(field.getType())) {
					@SuppressWarnings("unchecked")
					Optional<Object> value = (Optional<Object>) field.get(filterEntity);
					if (value.isPresent()) {
						expressions
								.add(factory.eq(AbstractQueryBuilder.makeVar(field.getName()), select.makeNode(value)));
					} else {
						TriplePath triplePath = select.makeTriplePath(idVar, pathPattern, Node.ANY);
						expressions.add(factory.notexists(new SelectBuilder().addWhere(triplePath)));
					}
				} else {
					Object value = field.get(filterEntity);
					expressions.add(factory.eq(AbstractQueryBuilder.makeVar(field.getName()), select.makeNode(value)));
				}
			}
		}

		return expressions.stream().reduce(factory::and).orElse(factory.asExpr(true));
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

	/**
	 * Inserts public fields of objects into a {@link Model} via SPARQL.
	 * 
	 * The SPARQL query will be build automatically using field types and
	 * annotations with {@link SparqlNamespace} and {@link SparqlPattern}. For
	 * insert queries, {@link SparqlPattern#value} is restricted to simple
	 * properties.
	 * <p>
	 * All public fields of each object must not be null. Field values will be
	 * translated into literals with according datatype, except of fields with one
	 * of the following types:
	 * <dl>
	 * <dt>{@link Collection}
	 * <dd>Each element will be translated into an literal or resource and inserted
	 * with an separate statement. Empty collections are permitted.
	 * <dt>{@link Optional}
	 * <dd>If present, the enclosed element will be translated into an literal or
	 * resource and inserted. If empty, nothing will be inserted.
	 * <dt>{@link Resource}
	 * <dd>The field value will be translated into a resource.
	 * </dl>
	 * 
	 * @param <T>     the type of the objects
	 * @param objects the objects to insert
	 * @param target  the {@link Model} to insert the objects into
	 */
	public static <T extends AbstractSparqlEntity> void insert(Collection<T> objects, Model target) {
		if (objects.isEmpty()) {
			return;
		}

		T prototype = objects.stream().findAny().get();
		Field[] fields = prototype.getClass().getFields();

		// manage prefixes
		Prologue prologue = new Prologue();
		for (SparqlNamespace namespaceAnnotation : prototype.getClass().getAnnotationsByType(SparqlNamespace.class)) {
			prologue.setPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
		}

		UpdateBuilder update = new UpdateBuilder(prologue.getPrefixMapping());

		for (T resource : objects) {

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

	/**
	 * Inserts public fields of the object into a {@link Model} via SPARQL.
	 * 
	 * This is a shortcut for passing a singleton {@link Collection} to
	 * {@link SparqlEntityManager#insert(Collection, Model)}.
	 * <p>
	 * 
	 * @param <T>    the type of the objects
	 * @param object the object to insert
	 * @param target the {@link Model} to insert the objects into
	 */
	public static <T extends AbstractSparqlEntity> void insert(T object, Model target) {
		insert(Collections.singleton(object), target);
	}

	private static Node property(Field field, Prologue prologue) throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = field.getAnnotation(SparqlPattern.class).value();
			Path path = PathParser.parse(propertyAnnotation, prologue);
			if (path instanceof P_Link) {
				return ((P_Link) path).getNode();
			} else {
				throw new IllegalArgumentException(
						String.format("Illegal %s annotation for member %s: Expected single property.",
								SparqlPattern.class.getSimpleName(), field.getName()));
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					SparqlPattern.class.getSimpleName(), field.getName()));
		}
	}

	private static Path propertyPath(Field field, PrefixMapping prefixMapping)
			throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = field.getAnnotation(SparqlPattern.class).value();
			return PathParser.parse(propertyAnnotation, prefixMapping);
		} catch (NullPointerException e) {
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					SparqlPattern.class.getSimpleName(), field.getName()));
		} catch (QueryException e) {
			throw new IllegalArgumentException(
					String.format("Illegal %s annotation for member %s: Expected property path.",
							SparqlPattern.class.getSimpleName(), field.getName()),
					e);
		}
	}

	/**
	 * Selects objects of a certain class form a {@link Model} via SPARQL filtered
	 * by given objects of that class.
	 * 
	 * The SPARQL query will be build automatically using field types and
	 * annotations with {@link SparqlNamespace} and {@link SparqlPattern}. For
	 * select queries, {@link SparqlPattern#value} permits simple properties and
	 * property paths.
	 * <p>
	 * The results will be filtered using the given objects. Each given object will
	 * be translated into a filter expression. Result objects must match at least
	 * one of filter expression. To match a filter, the all fields of the result
	 * objects must be match the according fields of the given object that are not
	 * null. Result fields matched if they are equal to the literal representation
	 * of the given object, except of fields with one of the following types:
	 * <dl>
	 * <dt>{@link Collection}
	 * <dd>A result field matches, if for each element of the given object a
	 * matching statement for the result object exists. Empty collections are
	 * permitted, but will have no effect.
	 * <dt>{@link Optional}
	 * <dd>If present, a result field matches, if the result field matches the
	 * enclosed element. If empty, a result field matches, if no value for the field
	 * exist.
	 * <dt>{@link Resource}
	 * <dd>A result field matches, if it represents the same resource.
	 * </dl>
	 * 
	 * TODO javadoc for return and throws
	 * 
	 * @param <T>           the type of the objects
	 * @param filterObjects the objects to use for filtering the result
	 * @param source        the {@link Model} select the objects from
	 * @return the selected objects
	 * @throws ReflectiveOperationException
	 * @throws IllegalStateException
	 * @throws NullPointerException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractSparqlEntity> Set<T> select(Collection<T> filterObjects, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		if (filterObjects.isEmpty()) {
			return Collections.emptySet();
		}

		T prototype = filterObjects.stream().findAny().get();

		// get plain query
		SelectBuilder select = selectQuery(prototype.getClass());
		ExprFactory expression = select.getExprFactory();

		// add entity filters to query
		Collection<Expr> entityExpressions = new ArrayList<>();
		for (T filterObject : filterObjects) {
			entityExpressions.add(objectFilter(filterObject, select));
		}
		entityExpressions.stream().reduce(expression::or).ifPresent(select::addFilter);

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
							// TODO is this possible?
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

	public static <T extends AbstractSparqlEntity> Set<T> select(T prototype, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		return select(Collections.singleton(prototype), source);
	}

	private static <T extends AbstractSparqlEntity> SelectBuilder selectQuery(Class<T> type) {
		return SELECT_QUERY_CACHE.computeIfAbsent(type, (t) -> {
			SelectBuilder select = new SelectBuilder();

			// add prefixes
			for (SparqlNamespace namespaceAnnotation : type.getAnnotationsByType(SparqlNamespace.class)) {
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
