package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.ProcessorParameters;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class ProcessingParameter extends AbstractEntityWithUUID {

	@Convert(converter = ProcessingParameterConverter.class)
	private ProcessorParameters parameters; // TODO empty or null?

	@ManyToOne
	private ProcessingConfiguration configuration;

	public ProcessingParameter() {
	}

	public ProcessingParameter(Class<Processor<?>> processorClass) throws InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.parameters = Processor.getDefaultParameters(processorClass);
	}

	private ProcessingParameter(ProcessingParameter original) {
		this.parameters = original.parameters; // TODO deep copy required?
	}

	public ProcessorParameters getParameters() {
		return parameters;
	}

	@SuppressWarnings("unchecked")
	public <P extends ProcessorParameters> P getParameters(Class<P> processorParametersClass) {
		return (P) parameters;
	}

	public void setParameters(ProcessorParameters processorParameters) {
		this.parameters = processorParameters;
	}

	/**
	 * @param keys keys to use during traverse
	 * @param gap  number of keys to stop earlier
	 * @return value of addressed object
	 * @throws NoSuchFieldException   if a given key was not found
	 * @throws IllegalAccessException if access to a given key was not permitted
	 * @throws SecurityException
	 */
	private Object get(String[] keys, int gap) throws NoSuchFieldException, IllegalAccessException, SecurityException {
		Object object = this.parameters;
		for (int i = 0; i < keys.length - 1 - gap; i++) {
			object = object.getClass().getDeclaredField(keys[i]).get(object);
		}
		return object;
	}

	/**
	 * @param path       path of keys to traverse
	 * @param returnType type of the returned instance
	 * @return value of addressed object
	 * @throws NoSuchFieldException   if a given key was not found
	 * @throws IllegalAccessException if access to a given key was not permitted
	 * @throws SecurityException
	 */
	public <T> T get(String path, Class<T> returnType)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		return returnType.cast(get(splitPath(path), 0));
	}

	public Class<?> getType(String path) throws NoSuchFieldException, SecurityException {
		return getType(splitPath(path));
	}

	public Class<?> getType(String[] keys) throws NoSuchFieldException, SecurityException {
		Class<?> type = this.parameters.getClass();
		for (int i = 0; i < keys.length - 1; i++) {
			type = type.getClass().getDeclaredField(keys[i]).getType();
		}
		return type;
	}

	/**
	 * @param path path of keys to check, keys are separated by "{@code .}"
	 * @return {@code true}, iff a parameter with a path of the given keys is set
	 * @throws IllegalAccessException
	 * @throws SecurityException
	 */
	public boolean containsKey(String path) throws IllegalAccessException, SecurityException {
		try {
			get(splitPath(path), 0);
			return true;
		} catch (NoSuchFieldException | NullPointerException e) {
			return false;
		}
	}

	public ProcessingParameter copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return new ProcessingParameter(this);
	}

	public ProcessingParameter put(String path, Object value)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		return put(splitPath(path), value);
	}

	private ProcessingParameter put(String[] keys, Object value)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		Object object = get(keys, 1);
		Field field = object.getClass().getDeclaredField(keys[keys.length - 1]);
		field.set(object, value);
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
		return String.format("ProcessingParameter[id=%s, parameter=%s]", this.id, this.parameters);
	}

}
