package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractProcessor implements Processor {

	private Map<String, Object> properties;
	protected ProgressListener listener;

	@Override
	public RdfGraph call() throws Exception {
		try {
			RdfGraph resultGraph = computeResultGraph();
			this.listener.onSuccess();
			return resultGraph;
		} catch (Exception e) {
			this.listener.onFailure(e);
			throw new Exception("Failed to provide a RdfGraph.", e);
		}
	}

	protected abstract RdfGraph computeResultGraph() throws Exception;

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

	protected <T> Optional<T> getOptionalProperty(String key, Class<T> type) {
		Object value = this.properties.get(key);
		if (Objects.isNull(value)) {
			return Optional.empty();
		} else if (type.isAssignableFrom(value.getClass())) {
			return Optional.of(type.cast(value));
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
