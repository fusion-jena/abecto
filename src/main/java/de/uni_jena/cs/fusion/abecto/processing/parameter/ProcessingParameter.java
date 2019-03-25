package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Convert;
import javax.persistence.Entity;

import org.apache.commons.lang3.reflect.TypeLiteral;

import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class ProcessingParameter extends AbstractEntityWithUUID {

	@Convert(converter = ProcessingParameterConverter.class)
	private Map<String, Object> parameter = new HashMap<String, Object>();

	public <T> void set(String key, Object value) {
		parameter.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key, TypeLiteral<T> type) {
		Object value = this.parameter.get(key);
		Objects.requireNonNull(value, "Missing value of parameter \"" + key + "\".");
		return (T) value;
	}

	public Map<String, Object> getAll() {
		return this.parameter;
	}

}
