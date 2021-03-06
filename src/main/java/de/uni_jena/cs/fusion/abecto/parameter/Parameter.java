/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.parameter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class Parameter extends AbstractEntityWithUUID {

	@Column(columnDefinition="CLOB")
	@Convert(converter = ParameterConverter.class)
	private ParameterModel parameters;

	public Parameter() {
	}

	public Parameter(Class<Processor<?>> processorClass) throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		this.parameters = Processor.getDefaultParameters(processorClass);
	}

	public Parameter(ParameterModel parameters) {
		this.parameters = parameters;
	}

	private Parameter(Parameter original) {
		this.parameters = original.parameters; // TODO deep copy required?
	}

	public ParameterModel getParameters() {
		return parameters;
	}

	@SuppressWarnings("unchecked")
	public <P extends ParameterModel> P getParameters(Class<P> processorParametersClass) {
		return (P) parameters;
	}

	public void setParameters(ParameterModel processorParameters) {
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
		for (int i = 0; i < keys.length - gap; i++) {
			object = object.getClass().getDeclaredField(keys[i]).get(object);
		}
		return object;
	}

	/**
	 * @param path path of keys to traverse
	 * @return value of addressed object
	 * @throws NoSuchFieldException   if a given key was not found
	 * @throws IllegalAccessException if access to a given key was not permitted
	 * @throws SecurityException
	 */
	public Object get(String path) throws NoSuchFieldException, IllegalAccessException, SecurityException {
		return get(parsePath(path), 0);
	}

	public Class<?> getType(String path) throws NoSuchFieldException, SecurityException {
		return getType(parsePath(path));
	}

	public Class<?> getType(String[] keys) throws NoSuchFieldException, SecurityException {
		Class<?> type = this.parameters.getClass();
		for (int i = 0; i < keys.length; i++) {
			type = type.getDeclaredField(keys[i]).getType();
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
			return get(parsePath(path), 0) != null;
		} catch (NoSuchFieldException | NullPointerException e) {
			return false;
		}
	}

	public Parameter copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return new Parameter(this);
	}

	public Parameter put(String path, Object value)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		return put(parsePath(path), value);
	}

	private Parameter put(String[] keys, Object value)
			throws NoSuchFieldException, IllegalAccessException, SecurityException {
		Object object = get(keys, 1);
		Field field = object.getClass().getDeclaredField(keys[keys.length - 1]);
		field.set(object, value);
		return this;
	}

	private String[] parsePath(String path) {
		if (path.isEmpty()) {
			return new String[] {};
		} else {
			return path.split("\\.");
		}
	}

}
