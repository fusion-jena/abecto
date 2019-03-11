package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang3.reflect.TypeLiteral;

@Entity
public class ProcessingParameter {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long processingParameterId;

	@Convert(converter = ProcessingParameterConverter.class)
	private Map<String, Object> parameter = new HashMap<String, Object>();

	public <T> void set(String key, Object value) {
		parameter.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key, TypeLiteral<T> type) {
		Object value = parameter.get(key);
		if (type.getType().getClass().isInstance(value)) {
			return (T) parameter.get(key);
		} else {
			throw new IllegalArgumentException(
					"Value of key \"" + key + "\" has not the type: " + type.getClass().toGenericString());
		}
	}

	public Map<String, Object> getAll() {
		return this.parameter;
	}

}
