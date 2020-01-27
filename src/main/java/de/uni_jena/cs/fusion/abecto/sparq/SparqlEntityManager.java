package de.uni_jena.cs.fusion.abecto.sparq;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.apache.jena.riot.RiotException;
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

	private final static Map<Class<?>, SelectBuilder> SELECT_QUERY_CACHE = new HashMap<>();

	private static void addInsert(UpdateBuilder update, Node subject, Node predicate, Node object) {
		update.addInsert(new Triple(subject, predicate, object));
	}

	private static <T> Expr getSelectFilter(T filterEntity, SelectBuilder select) throws ReflectiveOperationException,
			ARQInternalErrorException, IllegalArgumentException, NullPointerException {
		Collection<Expr> expressions = new ArrayList<>();
		ExprFactory factory = select.getPrologHandler().getExprFactory();

		for (Field field : filterEntity.getClass().getFields()) {
			if (field.get(filterEntity) != null) {
				Object value = field.get(filterEntity);
				if (value instanceof Collection) {
					SelectBuilder existsClause = new SelectBuilder();
					for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
						Path predicate = getAnnotationPropertyPath(field, annotation,
								select.getPrologHandler().getPrefixes());
						if (isAnnotationWithSubject(annotation)) {
							for (Object element : (Collection<?>) value) {

								// use element as object
								Node subject = AbstractQueryBuilder.makeVar(annotation.subject());
								Node object = select.makeNode(element);

								// add triple
								existsClause.addWhere(select.makeTriplePath(subject, predicate, object));
							}
						} else if (isAnnotationWithObject(annotation)) {
							throw new IllegalArgumentException(String.format(
									"Illegal annotation for %s: Omission of annotation subject permitted only for Resource fields.",
									field.getName()));
						} else {
							throw new IllegalArgumentException(String.format(
									"Missing annotation for %s: Either subject or object required.", field.getName()));
						}
					}
					expressions.add(factory.exists(existsClause));
				} else {
					expressions.add(factory.eq(AbstractQueryBuilder.makeVar(field.getName()), select.makeNode(value)));
				}
			}
		}

		return expressions.stream().reduce(factory::and).orElse(factory.asExpr(true));
	}

	private static Object getFieldValue(RDFNode node, Field field) {
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
	 * Public {@link Resource} and {@link Optional}<{@link Resource}> fields that
	 * are null will be treated as blank nodes. Other public fields must not be null
	 * and will be translated into literals with according datatype, except of
	 * fields with one of the following types:
	 * <dl>
	 * <dt>{@link Collection}
	 * <dd>Each element will be translated into an literal or resource and inserted
	 * with an separate statement. Empty collections are permitted.
	 * <dt>{@link Optional}
	 * <dd>If present, the enclosed element will be translated into an literal or
	 * resource and inserted. If empty, nothing will be inserted.
	 * </dl>
	 * 
	 * @param <T>     the type of the objects
	 * @param objects the objects to insert
	 * @param target  the {@link Model} to insert the objects into
	 */
	public static <T> void insert(Collection<T> objects, Model target) {
		if (objects.isEmpty()) {
			return;
		}

		T prototype = objects.stream().findAny().get();
		Field[] fields = prototype.getClass().getFields();

		// manage prefixes
		
		Prologue prologue = new Prologue();
		prologue.setBaseURI("");
		for (SparqlNamespace namespaceAnnotation : prototype.getClass().getAnnotationsByType(SparqlNamespace.class)) {
			prologue.setPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
		}

		UpdateBuilder update = new UpdateBuilder(prologue.getPrefixMapping());

		for (T resource : objects) {

			// categorize and transform field values
			Map<Field, Node> resources = new HashMap<>();
			Map<Field, Node> literals = new HashMap<>();
			Map<Field, Node> optionalResources = new HashMap<>();
			Map<Field, Set<Node>> collections = new HashMap<>();
			for (Field field : fields) {
				try {
					Object value = field.get(resource);
					if (value == null) {
						if (Resource.class.isAssignableFrom(field.getType())) {
							resources.put(field, NodeFactory.createBlankNode());
						} else {
							throw new NullPointerException(
									String.format("Missing value for member %s.", field.getName()));
						}
					} else if (value instanceof Resource) {
						resources.put(field, update.makeNode(value));
					} else if (value instanceof Optional) {
						if (((Optional<?>) value).isPresent()) {
							Object enclosedValue = ((Optional<?>) value).get();
							if (enclosedValue instanceof Resource) {
								// add to resources, not to optionalResources, to assure insertion of provided
								// data
								resources.put(field, update.makeNode(enclosedValue));
							} else {
								literals.put(field, update.makeNode(enclosedValue));
							}
						} else if (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
								.equals(Resource.class)) {
							optionalResources.put(field, NodeFactory.createBlankNode());
						}
					} else if (value instanceof Collection) {
						if (((Collection<?>) value).stream().anyMatch(Objects::isNull)) {
							throw new NullPointerException(
									String.format("Null element contained in member collection %s.", field.getName()));
						}
						collections.put(field,
								((Collection<?>) value).stream().map(update::makeNode).collect(Collectors.toSet()));
					} else {
						literals.put(field, update.makeNode(value));
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Failed to access member " + field.getName(), e);
				}
			}

			List<String> requiredResources = new ArrayList<>();

			for (Field field : resources.keySet()) {
				for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
					validateAnnotation(annotation, field);
					Node property = getAnnotationProperty(field, annotation, prologue);
					if (isAnnotationWithSubject(annotation)) {
						Node subject = getAnnotationSubject(field, annotation, resources);
						Node object = resources.get(field);
						addInsert(update, subject, property, object);
						requiredResources.add(annotation.subject());
					} else if (isAnnotationWithObject(annotation)) {
						Node subject = resources.get(field);
						Node object = getAnnotationObject(field, annotation, prologue);
						addInsert(update, subject, property, object);
					}
				}
			}

			for (Field field : literals.keySet()) {
				for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
					validateAnnotation(annotation, field);
					if (isAnnotationWithSubject(annotation)) {
						Node subject = getAnnotationSubject(field, annotation, resources);
						Node property = getAnnotationProperty(field, annotation, prologue);
						Node object = literals.get(field);
						addInsert(update, subject, property, object);
						requiredResources.add(annotation.subject());
					} else {
						throw new IllegalArgumentException(
								String.format("Illegal annotation for %s: Missing subject.", field.getName()));
					}
				}
			}

			for (Field field : collections.keySet()) {
				for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
					validateAnnotation(annotation, field);
					if (isAnnotationWithSubject(annotation)) {
						Node subject = getAnnotationSubject(field, annotation, resources);
						Node property = getAnnotationProperty(field, annotation, prologue);
						for (Node object : collections.get(field)) {
							addInsert(update, subject, property, object);
						}
						requiredResources.add(annotation.subject());
					} else {
						throw new IllegalArgumentException(
								String.format("Illegal annotation for %s: Missing subject.", field.getName()));
					}
				}
			}

			for (ListIterator<String> requiredResourcesIterator = requiredResources
					.listIterator(); requiredResourcesIterator.hasNext();) {
				String fieldName = requiredResourcesIterator.next();
				try {
					Field field = resource.getClass().getField(fieldName);
					if (optionalResources.containsKey(field)) {
						for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
							validateAnnotation(annotation, field);
							Node property = getAnnotationProperty(field, annotation, prologue);
							if (isAnnotationWithSubject(annotation)) {
								Node subject = getAnnotationSubject(field, annotation, resources);
								Node object = optionalResources.get(field);
								addInsert(update, subject, property, object);
								requiredResourcesIterator.add(annotation.subject());
							} else if (isAnnotationWithObject(annotation)) {
								Node subject = optionalResources.get(field);
								Node object = getAnnotationObject(field, annotation, prologue);
								addInsert(update, subject, property, object);
							}
						}
					}
				} catch (NoSuchFieldException | SecurityException e) {
					throw new IllegalStateException(String.format("Field %s not found.", fieldName), e);
				}
			}

		}

		// execute update
		UpdateAction.execute(update.buildRequest(), target);

	}

	private static boolean isAnnotationWithSubject(SparqlPattern annotation) {
		return annotation.object().isEmpty() && !annotation.subject().isEmpty();
	}

	private static boolean isAnnotationWithObject(SparqlPattern annotation) {
		return annotation.subject().isEmpty() && !annotation.object().isEmpty();
	}

	private static void validateAnnotation(SparqlPattern annotation, Field field) {
		if (annotation.subject().isEmpty() && annotation.object().isEmpty()
				|| !annotation.subject().isEmpty() && !annotation.object().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Illegal annotation for %s: Either subject or object required.", field.getName()));
		}
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
	public static <T> void insert(T object, Model target) {
		insert(Collections.singleton(object), target);
	}

	/**
	 * 
	 * @param field
	 * @param annotation
	 * @param resources
	 * @return
	 * @throws IllegalArgumentException
	 * @throws NullPointerException
	 * @throws NoSuchElementException   if subject field is not present or not a
	 *                                  resource
	 */
	private static Node getAnnotationSubject(Field field, SparqlPattern annotation, Map<Field, Node> resources)
			throws IllegalArgumentException, NullPointerException, NoSuchElementException {
		try {
			String subject = annotation.subject();
			if (subject.isEmpty()) {
				throw new IllegalArgumentException(
						String.format("Illegal %s annotation for member %s: Expected subject.",
								SparqlPattern.class.getSimpleName(), field.getName()));
			}
			// return value of the subject field
			return resources.entrySet().stream().filter((e) -> e.getKey().getName().equals(subject)).findAny()
					.orElseThrow().getValue();
		} catch (NullPointerException e) { // annotation == null
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					SparqlPattern.class.getSimpleName(), field.getName()));
		}
	}

	private static Node getAnnotationObject(Field field, SparqlPattern annotation, Prologue prologue)
			throws IllegalArgumentException, NullPointerException {
		try {
			String object = annotation.object();
			if (object.isEmpty()) {
				throw new IllegalArgumentException(
						String.format("Illegal %s annotation for member %s: Expected object.",
								SparqlPattern.class.getSimpleName(), field.getName()));
			}
			return NodeFactory.createURI(prologue.expandPrefixedName(object));
		} catch (NullPointerException e) {
			e.printStackTrace(System.out);
			throw new NullPointerException(String.format("Missing %s annotation for member %s.",
					SparqlPattern.class.getSimpleName(), field.getName()));
		} catch (RiotException e) {
			throw new IllegalArgumentException(String.format("Illegal %s annotation for member %s: Illegal object.",
					SparqlPattern.class.getSimpleName(), field.getName()));
		}
	}

	private static Node getAnnotationProperty(Field field, SparqlPattern annotation, Prologue prologue)
			throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = annotation.predicate();
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

	private static Path getAnnotationPropertyPath(Field field, SparqlPattern annotation, PrefixMapping prefixMapping)
			throws IllegalArgumentException, NullPointerException {
		try {
			String propertyAnnotation = annotation.predicate();
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
	 * one of filter expression. To match a filter, all fields of the result objects
	 * must match the according fields of the given object that are not null. Result
	 * fields match if they are equal to the literal representation of the given
	 * object, except of fields with one of the following types:
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
	 * TODO javadoc for throws
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
	public static <T> Set<T> select(Collection<T> filterObjects, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		if (filterObjects.isEmpty()) {
			return Collections.emptySet();
		}

		T prototype = filterObjects.stream().findAny().get();
		List<Field> fields = Arrays.asList(prototype.getClass().getFields());

		// get plain query
		SelectBuilder select = getSelectQuery(prototype.getClass());
		ExprFactory expressionFactory = select.getExprFactory();

		// add entity filters to query
		Collection<Expr> entityExpressions = new ArrayList<>();
		for (T filterObject : filterObjects) {
			entityExpressions.add(getSelectFilter(filterObject, select));
		}
		entityExpressions.stream().reduce(expressionFactory::or).ifPresent(select::addFilter);

		// execute query
		ResultSet queryResults = QueryExecutionFactory.create(select.build(), source).execSelect();

		boolean classContainsCollection = fields.stream()
				.anyMatch((field -> Collection.class.isAssignableFrom(field.getType())));

		// result entity indices for each field by value
		Map<Field, Map<RDFNode, Set<T>>> resultEntityIndices = new HashMap<>();
		for (Field field : fields) {
			if (!Collection.class.isAssignableFrom(field.getType())) {
				resultEntityIndices.put(field, new HashMap<>());
			}
		}

		// generate result set
		Set<T> entities = new HashSet<T>();
		while (queryResults.hasNext()) {
			QuerySolution queryResult = queryResults.next();
			boolean firstVisit;

			// get entity by lookup for collection building or instantiation
			T entity;
			if (classContainsCollection) {
				Set<T> matchingEntities = fields.stream()
						.filter((field) -> !Collection.class.isAssignableFrom(field.getType()))
						.map((field) -> resultEntityIndices.get(field).getOrDefault(queryResult.get(field.getName()),
								Collections.emptySet()))
						.reduce((a, b) -> {
							a.retainAll(b);
							return a;
						}).orElse(Collections.emptySet());
				if (!matchingEntities.isEmpty()) {
					entity = matchingEntities.iterator().next();
					firstVisit = false;
				} else {
					entity = getNewEntity(prototype);
					firstVisit = true;
				}
			} else {
				entity = getNewEntity(prototype);
				firstVisit = true;
			}

			for (Field field : prototype.getClass().getFields()) {
				RDFNode node = queryResult.get(field.getName());
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection<Object> collection = (Collection<Object>) field.get(entity);
					if (collection != null) {
						if (node != null) {
							collection.add(getFieldValue(node, field));
						}
					} else {
						throw new NullPointerException(
								String.format("Member collection %s not initialized.", field.getName()));
					}
				} else {
					Object newValue;
					if (Optional.class.isAssignableFrom(field.getType())) {
						if (node != null) {
							newValue = Optional.of(getFieldValue(node, field));
						} else {
							newValue = Optional.empty();
						}
					} else {
						if (node != null) {
							newValue = getFieldValue(node, field);
						} else {
							// this might occur, if there are field not connected to other fields
							throw new IllegalStateException(
									String.format("Missing value for non optional member %s.", field.getName()));
						}
					}
					if (firstVisit) {
						field.set(entity, newValue);
						resultEntityIndices.get(field).computeIfAbsent(node, (n) -> new HashSet<T>()).add(entity);
					} else if (!field.get(entity).equals(newValue)) {
						throw new IllegalStateException(
								String.format("Multiple values for functional field %s: \"%s\", \"%s\".",
										field.getName(), field.get(entity), newValue));
					}
				}
			}
			entities.add(entity); // do after creating entity manipulations to not change hash values
		}

		return entities;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getNewEntity(T prototype) throws ReflectiveOperationException {
		return (T) prototype.getClass().getDeclaredConstructor().newInstance();
	}

	/**
	 * Selects objects of a certain class form a {@link Model} via SPARQL filtered
	 * by a given object of that class.
	 * 
	 * This is a shortcut for passing a singleton {@link Collection} to
	 * {@link SparqlEntityManager#select(Collection, Model)}.
	 * <p>
	 * 
	 * @param <T>       the type of the objects
	 * @param prototype the object to use for filtering the result
	 * @param source    the {@link Model} select the objects from
	 * @return the selected objects
	 * @throws ReflectiveOperationException
	 * @throws IllegalStateException
	 * @throws NullPointerException
	 */
	public static <T> Set<T> select(T prototype, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		return select(Collections.singleton(prototype), source);
	}

	private static <T> SelectBuilder getSelectQuery(Class<T> type) {
		return SELECT_QUERY_CACHE.computeIfAbsent(type, (t) -> {
			SelectBuilder select = new SelectBuilder();

			// add prefixes
			for (SparqlNamespace namespaceAnnotation : type.getAnnotationsByType(SparqlNamespace.class)) {
				select.addPrefix(namespaceAnnotation.prefix(), namespaceAnnotation.namespace());
			}

			for (Field field : type.getFields()) {
				for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {
					Var subject;
					Path predicate = getAnnotationPropertyPath(field, annotation,
							select.getPrologHandler().getPrefixes());
					Var object;

					if (isAnnotationWithObject(annotation)) {
						// use field as subject
						if (!Resource.class.isAssignableFrom(field.getType())) {
							throw new IllegalArgumentException(String.format(
									"Illegal annotation for %s: Omission of annotation subject permitted only for Resource fields.",
									field.getName()));
						}
						subject = AbstractQueryBuilder.makeVar(field.getName());
						object = AbstractQueryBuilder.makeVar(annotation.object());
					} else if (isAnnotationWithSubject(annotation)) {
						// use field as object
						subject = AbstractQueryBuilder.makeVar(annotation.subject());
						object = AbstractQueryBuilder.makeVar(field.getName());
					} else {
						throw new IllegalArgumentException(String.format(
								"Missing annotation for %s: Either subject or object required.", field.getName()));
					}
					// add triple
					TriplePath triplePath = select.makeTriplePath(subject, predicate, object);
					if (Collection.class.isAssignableFrom(field.getType())
							|| Optional.class.isAssignableFrom(field.getType())) {
						select.addOptional(triplePath);
					} else {
						select.addWhere(triplePath);
					}
				}
			}

			return select;
		}).clone();
	}

	private SparqlEntityManager() {
		// prevent instantiation
	}
}
