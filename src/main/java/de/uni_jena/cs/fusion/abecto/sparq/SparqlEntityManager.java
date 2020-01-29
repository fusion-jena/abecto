package de.uni_jena.cs.fusion.abecto.sparq;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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

		for (Field field : getPublicNonstaticFields(filterEntity)) {
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
				} else if (value instanceof Optional<?>) {
					if (((Optional<?>) value).isPresent()) {
						expressions.add(factory.eq(AbstractQueryBuilder.makeVar(field.getName()),
								select.makeNode(((Optional<?>) value).get())));
					} else {
						SelectBuilder notExistsClause = new SelectBuilder();
						for (SparqlPattern annotation : field.getAnnotationsByType(SparqlPattern.class)) {

							Node subject = AbstractQueryBuilder.makeVar(annotation.subject());
							Path predicate = getAnnotationPropertyPath(field, annotation,
									select.getPrologHandler().getPrefixes());

							notExistsClause.addWhere(select.makeTriplePath(subject, predicate, Node.ANY));
						}
						expressions.add(factory.notexists(notExistsClause));
					}
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
		List<Field> fields = getPublicNonstaticFields(prototype);

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
					Field field = getPublicNonstaticField(resource, fieldName);
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
	 * @param <T>           the type of the objects
	 * @param filterObjects the objects to use for filtering the result
	 * @param source        the {@link Model} select the objects from
	 * @return the selected objects
	 * @throws ReflectiveOperationException if creation of an object failed for
	 *                                      various reasons
	 * @throws IllegalStateException        if a value in the SPARQL query solution
	 *                                      has an inappropriate type
	 * @throws NullPointerException         if the model annotation is not
	 *                                      sufficient
	 */
	public static <T> Set<T> select(Collection<T> filterObjects, Model source)
			throws ReflectiveOperationException, IllegalStateException, NullPointerException {
		if (filterObjects.isEmpty()) {
			return Collections.emptySet();
		}

		T prototype = filterObjects.stream().findAny().get();
		List<Field> fields = getPublicNonstaticFields(prototype);

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
		ResultSet querySolutions = QueryExecutionFactory.create(select.build(), source).execSelect();

		// generate result set
		Set<T> results = new HashSet<T>();

		// get constructor for entities
		Constructor<T> constructor = getParameterizedConstructor(prototype)
				.orElseGet(() -> getPlainConstructor(prototype));

		if (fields.stream().anyMatch((field -> Collection.class.isAssignableFrom(field.getType())))) {
			// class has member with type collection

			// create object index
			Map<String, Map<Object, Set<T>>> objectIndex = createObjectIndex(prototype);

			// iterate solutions
			while (querySolutions.hasNext()) {

				// get field values from query solution
				Map<String, Object> fieldValues = getFieldValues(prototype, querySolutions.next());

				// try to find matching object in object index
				Optional<T> existingEntity = searchInObjectIndex(prototype, objectIndex, fieldValues);

				if (existingEntity.isPresent()) {
					// matching object found

					// update object
					updateObject(existingEntity.get(), fieldValues);

				} else {
					// no matching object found

					// create new object
					T entity = createObject(constructor, fieldValues);

					// add object to object index
					addToObjectIndex(entity, objectIndex);

					// add object to results
					results.add(entity);
				}
			}
		} else {
			// class has no member with type collection

			// iterate solutions
			while (querySolutions.hasNext()) {

				// get field values from query solution
				Map<String, Object> fieldValues = getFieldValues(prototype, querySolutions.next());

				// create new object
				T entity = createObject(constructor, fieldValues);

				// add object to results
				results.add(entity);
			}
		}
		return results;
	}

	/**
	 * Provides a {@link Map} of {@link Field} values based on a
	 * {@link QuerySolution} for an object.
	 * 
	 * Creates a new {@link ArrayList} for {@link Collection} fields. During
	 * population of an object, the returned {@link ArrayList} should be added to
	 * the object using {@link Collection#addAll(Collection)} to avoid conflicts
	 * with actual collection type of the field.
	 * 
	 * @param <T>           type of the new object
	 * @param prototype     the prototype of the object
	 * @param querySolution the {@link QuerySolution} to obtain the values from
	 * @return field values for an object
	 */
	private static <T> Map<String, Object> getFieldValues(T prototype, QuerySolution querySolution) {
		Map<String, Object> fieldValues = new HashMap<>();
		for (Field field : getPublicNonstaticFields(prototype)) {
			RDFNode node = querySolution.get(field.getName());
			if (Collection.class.isAssignableFrom(field.getType())) {
				Collection<Object> collection = new ArrayList<Object>();
				if (node != null) {
					collection.add(getFieldValue(node, field));
				}
				fieldValues.put(field.getName(), collection);
			} else {
				if (Optional.class.isAssignableFrom(field.getType())) {
					if (node != null) {
						fieldValues.put(field.getName(), Optional.of(getFieldValue(node, field)));
					} else {
						fieldValues.put(field.getName(), Optional.empty());
					}
				} else {
					if (node != null) {
						fieldValues.put(field.getName(), getFieldValue(node, field));
					} else {
						// this might occur, if there is a field not connected to other fields
						throw new IllegalStateException(
								String.format("Missing value for non optional member %s.", field.getName()));
					}
				}
			}
		}

		return fieldValues;
	}

	/**
	 * Provides the constructor with parameters matching the class members.
	 * 
	 * @param <T>       type of the objects to construct
	 * @param prototype the prototype of the objects to construct
	 * @return constructor with matching parameters
	 */
	@SuppressWarnings("unchecked")
	private static <T> Optional<Constructor<T>> getParameterizedConstructor(T prototype) {
		for (Constructor<?> constructor : prototype.getClass().getConstructors()) {
			if (constructor.getParameterCount() != getPublicNonstaticFields(prototype).size()) {
				// constructor has wrong parameter count
				continue;
			}
			for (Parameter parameter : constructor.getParameters()) {
				Member member = parameter.getAnnotation(Member.class);
				if (member == null) {
					// parameter not assigned to a member
					continue;
				}
				try {
					Field field = getPublicNonstaticField(prototype, member.value());
					if (!field.getType().isAssignableFrom(parameter.getType())) {
						// parameter type does not fit to member type
						continue;
					}
				} catch (NoSuchFieldException | SecurityException e) {
					// member name does not match parameter annotation
					continue;
				}
			}
			// found suitable constructor
			return Optional.of((Constructor<T>) constructor);
		}
		return Optional.empty();
	}

	private static List<Field> getPublicNonstaticFields(Object object) {
		List<Field> fields = new ArrayList<>();
		for (Field field : Arrays.asList(object.getClass().getFields())) {
			if (!Modifier.isStatic(field.getModifiers())) {
				fields.add(field);
			}
		}
		return fields;
	}

	private static Field getPublicNonstaticField(Object object, String name)
			throws NoSuchFieldException, SecurityException {
		Field field = object.getClass().getField(name);
		if (Modifier.isStatic(field.getModifiers())) {
			throw new NoSuchFieldException(String.format("Field % is static.", name));
		}
		return field;
	}

	/**
	 * Provides the constructor without parameters.
	 * 
	 * @param <T>       type of the objects to construct
	 * @param prototype the prototype of the objects to construct
	 * @return constructor without parameters
	 */
	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getPlainConstructor(T prototype) {
		try {
			return (Constructor<T>) prototype.getClass().getConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Provides the matching object in the object index, if exists.
	 * 
	 * @param <T>
	 * @param prototype
	 * @param objectIndex
	 * @param fieldValues
	 * @return
	 */
	private static <T> Optional<T> searchInObjectIndex(T prototype, Map<String, Map<Object, Set<T>>> objectIndex,
			Map<String, Object> fieldValues) {
		return objectIndex.keySet().stream().map((fieldName) -> objectIndex.get(fieldName)
				.getOrDefault(fieldValues.get(fieldName), Collections.emptySet())).reduce((a, b) -> {
					a.retainAll(b);
					return a;
				}).filter(Predicate.not(Collection::isEmpty)).map((set) -> set.iterator().next());
	}

	private static <T> void addToObjectIndex(T entity, Map<String, Map<Object, Set<T>>> objectIndex)
			throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getPublicNonstaticFields(entity)) {
			if (!Collection.class.isAssignableFrom(field.getType())) {
				objectIndex.get(field.getName()).computeIfAbsent(field.get(entity), (key) -> new HashSet<>())
						.add(entity);
			}
		}
	}

	private static <T> Map<String, Map<Object, Set<T>>> createObjectIndex(T prototype) {
		Map<String, Map<Object, Set<T>>> objectIndex = new HashMap<>();
		for (Field field : getPublicNonstaticFields(prototype)) {
			if (!Collection.class.isAssignableFrom(field.getType())) {
				objectIndex.put(field.getName(), new HashMap<>());
			}
		}
		return objectIndex;
	}

	@SuppressWarnings("unchecked")
	private static <T> void updateObject(T object, Map<String, Object> fieldValues)
			throws IllegalAccessException, SecurityException, IllegalArgumentException, NoSuchFieldException {
		for (Entry<String, Object> fieldValue : fieldValues.entrySet()) {
			if (fieldValue.getValue() instanceof Collection) {
				((Collection<Object>) getPublicNonstaticField(object, fieldValue.getKey()).get(object))
						.addAll((Collection<Object>) fieldValue.getValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <I, O> O convert(I value, Class<O> out) {
		if (out.isInstance(value)) {
			return out.cast(value);
		}
		if (value instanceof Number && Number.class.isAssignableFrom(out)) {
			if (Long.class.isAssignableFrom(out)) {
				return (O) Long.valueOf(((Number) value).longValue());
			}
			if (Integer.class.isAssignableFrom(out)) {
				return (O) Integer.valueOf(((Number) value).intValue());
			}
			if (Byte.class.isAssignableFrom(out)) {
				return (O) Byte.valueOf(((Number) value).byteValue());
			}
			if (Short.class.isAssignableFrom(out)) {
				return (O) Short.valueOf(((Number) value).shortValue());
			}
			if (Double.class.isAssignableFrom(out)) {
				return (O) Double.valueOf(((Number) value).doubleValue());
			}
			if (Float.class.isAssignableFrom(out)) {
				return (O) Float.valueOf(((Number) value).floatValue());
			}
		}
		// otherwise give them a try
		return (O) value;
	}

	@SuppressWarnings("unchecked")
	private static <T> T createObject(Constructor<T> constructor, Map<String, Object> fieldValues)
			throws ReflectiveOperationException {
		T object;
		if (constructor.getParameterCount() == 0) {
			// create instance
			object = constructor.newInstance();

			// set members
			for (String fieldName : fieldValues.keySet()) {
				Field field = getPublicNonstaticField(object, fieldName);
				if (fieldValues.get(fieldName) instanceof Collection<?>) {
					Collection<Object> collection = ((Collection<Object>) field.get(object));
					// if a collection add all values
					((Collection<Object>) fieldValues.get(fieldName)).stream().map((v) -> convert(v, field.getType()))
							.forEach(collection::add);
				} else {
					// if not a collection set value
					field.set(object, convert(fieldValues.get(fieldName), field.getType()));
				}
			}
		} else {
			// initialize parameter value array
			Object[] parameterValues = new Object[constructor.getParameterCount()];

			// set parameters
			Parameter[] parameters = constructor.getParameters();
			for (int i = 0; i < constructor.getParameterCount(); i++) {
				parameterValues[i] = fieldValues.get(parameters[i].getAnnotation(Member.class).value());
			}

			// create instance using the parameter values
			object = constructor.newInstance(parameterValues);
		}
		return object;
	}

	/**
	 * Selects one object of a certain class form a {@link Model} via SPARQL
	 * filtered by a given object of that class.
	 * 
	 * This is a shortcut for
	 * <ol>
	 * <li>calling {@link SparqlEntityManager#select(Collection, Model)},
	 * <li>ensuring that there is exact one result,
	 * <li>and unwrapping them.
	 * </ol>
	 * 
	 * @param <T>       the type of the objects
	 * @param prototype the object to use for filtering the result
	 * @param source    the {@link Model} select the objects from
	 * @return the selected object
	 * @throws ReflectiveOperationException if creation of an object failed for
	 *                                      various reasons
	 * @throws IllegalStateException        if a value in the SPARQL query solution
	 *                                      has an inappropriate type, or if
	 *                                      multiple results have been found
	 * @throws NullPointerException         if the model annotation is not
	 *                                      sufficient
	 * @throws NoSuchElementException       if no result has been found
	 */
	public static <T> T selectOne(T prototype, Model source)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		Set<T> result = select(prototype, source);
		if (result.isEmpty()) {
			throw new NoSuchElementException();
		}
		if (result.size() > 1) {
			throw new IllegalStateException("Selected multiple results.");
		}
		return result.iterator().next();
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
	 * @throws ReflectiveOperationException if creation of an object failed for
	 *                                      various reasons
	 * @throws IllegalStateException        if a value in the SPARQL query solution
	 *                                      has an inappropriate type
	 * @throws NullPointerException         if the model annotation is not
	 *                                      sufficient
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
