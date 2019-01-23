package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Objects;

import de.uni_jena.cs.fusion.abecto.progress.ProgressListener;

public abstract class AbstractProcessor implements Processor {

	private Map<String, Object> properties;
	protected ProgressListener listener;

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	protected <T> T getProperty(String key, Class<T> type) {
		Object value = this.properties.get(key);
		Objects.requireNonNull(value, "Missing value of property \"" + key + "\".");
		if (type.isAssignableFrom(value.getClass())) {
			return type.cast(value);
		} else {
			throw new ClassCastException(
					"Failed to cast value of property \"" + key + "\" to \"" + type.getName() + "\".");
		}
	}

	@Override
	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}
}
