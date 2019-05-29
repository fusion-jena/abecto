package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.reflect.TypeLiteral;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class ProcessingParameter extends AbstractEntityWithUUID {

	public final static TypeLiteral<List<Object>> LIST_TYPE = new TypeLiteral<List<Object>>() {};
	public final static TypeLiteral<Long> LONG_TYPE = new TypeLiteral<Long>() {};
	public final static TypeLiteral<Map<String, Object>> MAP_TYPE = new TypeLiteral<Map<String, Object>>() {};
	public final static TypeLiteral<String> STRING_TYPE = new TypeLiteral<String>() {};

	@Convert(converter = ProcessingParameterConverter.class)
	private Map<String, Object> parameter = new HashMap<String, Object>();
	
	@ManyToOne
	private ProcessingConfiguration configuration;

	public ProcessingParameter() {}

	private ProcessingParameter(ProcessingParameter origina) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.parameter = deepCopy(parameter);
	}

	/**
	 * @param path path of keys to check, keys are separated by "{@code .}"
	 * @return {@code true}, iff a parameter with the given path is set
	 */
	public boolean containsKey(String path) {
		return containsKey(splitPath(path));
	}

	/**
	 * @param keys keys to check
	 * @return {@code true}, iff a parameter with a path of the given keys is set
	 */
	public boolean containsKey(String[] keys) {
		Map<String, Object> leafLevel = traverse(this.parameter, Arrays.copyOf(keys, keys.length - 1), false);
		return leafLevel.containsKey(keys[keys.length - 1]);
	}

	public ProcessingParameter copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return new ProcessingParameter(this);
	}

	@SuppressWarnings("unchecked")
	private <T> T deepCopy(T object) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		if (object.getClass().isPrimitive()) {
			return object;
		}
		if (object instanceof String) {
			return object;
		}
		if (object instanceof List) {
			@SuppressWarnings("rawtypes")
			List copy = (List) object.getClass().getDeclaredConstructor(new Class<?>[] {}).newInstance(new Object[] {});
			Iterator<?> iterator = ((List<?>) object).iterator();
			while (iterator.hasNext()) {
				copy.add(deepCopy(iterator.next()));
			}
			return (T) copy;
		}
		if (object instanceof Set) {
			@SuppressWarnings("rawtypes")
			Set copy = (Set) object.getClass().getDeclaredConstructor(new Class<?>[] {}).newInstance(new Object[] {});
			Iterator<?> iterator = ((Set<?>) object).iterator();
			while (iterator.hasNext()) {
				copy.add(deepCopy(iterator.next()));
			}
			return (T) copy;
		}
		if (object instanceof Map) {
			@SuppressWarnings("rawtypes")
			Map copy = (Map) object.getClass().getDeclaredConstructor(new Class<?>[] {}).newInstance(new Object[] {});
			Iterator<?> iterator = ((Map<String, ?>) object).entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, ?> entry = (Entry<String, ?>) iterator.next();
				copy.put(entry.getKey(), deepCopy(entry.getValue()));
			}
			return (T) copy;
		}
		throw new UnsupportedOperationException(
				String.format("Copping instance of %s not supported.", object.getClass()));
	}

	public <T> T get(String path, TypeLiteral<T> type) {
		return get(splitPath(path), type);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String[] keys, TypeLiteral<T> type) {
		if (keys.length == 0 && type.equals(MAP_TYPE)) {
			return (T) this.parameter;
		} else {
			Map<String, Object> leafLevel = traverse(this.parameter, Arrays.copyOf(keys, keys.length - 1), true);
			Object value = leafLevel.get(keys[keys.length - 1]);
			Objects.requireNonNull(value, new Supplier<String>() {
				@Override
				public String get() {
					return String.format("Missing value of parameter \"%s\".", String.join(".", keys));
				}
			});
			return (T) value;
		}
	}

	public List<Object> getList(String path) {
		return get(splitPath(path), LIST_TYPE);
	}

	public Long getLong(String path) {
		return get(splitPath(path), LONG_TYPE);
	}

	public Map<String, Object> getMap() {
		return this.parameter;
	}

	public Map<String, Object> getMap(String path) {
		return get(splitPath(path), MAP_TYPE);
	}

	public String getString(String path) {
		return get(splitPath(path), STRING_TYPE);
	}

	public ProcessingParameter put(String path, Object value) {
		return put(splitPath(path), value);
	}

	public ProcessingParameter put(String[] keys, Object value) {
		Map<String, Object> leafLevel = traverse(this.parameter, Arrays.copyOf(keys, keys.length - 1), true);
		leafLevel.put(keys[keys.length - 1], value);
		return this;
	}

	private String[] splitPath(String path) {
		if (path.isEmpty()) {
			return new String[] {};
		} else {
			return path.split("\\.");
		}
	}

	@Override
	public String toString() {
		return String.format("ProcessingParameter[id=%s, parameter=%s]", this.id, this.parameter);
	}

	/**
	 * 
	 * @param map    map (of maps (of maps (...)) to traverse, contained maps are
	 *               assumed to have Strings as keys
	 * @param keys   keys to use during traverse
	 * @param create if {@code true}, missing maps will be created
	 * @return value of deepest addressed map
	 * @throws NullPointerException
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> traverse(Map<String, Object> map, String[] keys, boolean create)
			throws NullPointerException, ClassCastException {
		Map<String, Object> currentLevel = this.parameter;
		int i;
		for (i = 0; i < keys.length - 1; i++) {
			if (create && !currentLevel.containsKey(keys[i])) {
				currentLevel.put(keys[i], new HashMap<String, Object>());
			}
			currentLevel = (Map<String, Object>) currentLevel.get(keys[i]);
		}
		return currentLevel;
	}

}
