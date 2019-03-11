package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.reflect.TypeLiteral;

import de.uni_jena.cs.fusion.abecto.processor.progress.NullProgressListener;
import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractProcessor implements Processor {

	/**
	 * <p>
	 * The current configuration properties of this processor.
	 * </p>
	 * 
	 * <p>
	 * Use {@link #getProperty(String, Class)} or
	 * {@link #getOptionalProperty(String, Class)} for type safe access without need
	 * of missing error exception handling.
	 * </p>
	 */
	private Map<String, Object> properties;
	/**
	 * The {@link ProgressListener} of this processor.
	 */
	protected ProgressListener listener = NullProgressListener.get();

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

	/**
	 * <p>
	 * Computes a {@link RdfGraph}, or throws an exception if unable to do so.
	 * </p>
	 * 
	 * <p>
	 * This method should be implemented instead of overwriting {@link call}. It
	 * will be called by {@link call}. Calling {@link ProgressListener#onSuccess()}
	 * or {@link ProgressListener#onFailure(Throwable)} is done by {@link call}.
	 * Therefore, only {@link ProgressListener#onProgress(long, long)} needs to be
	 * called here.
	 * </p>
	 * 
	 * @return computed {@link RdfGraph}
	 * @throws Exception
	 */
	protected abstract RdfGraph computeResultGraph() throws Exception;

	@Override
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	/**
	 * 
	 * Get an property value.
	 * 
	 * @param key  property name
	 * @param type expected type of the property value
	 * @return property value
	 * 
	 * @throws ClassCastException   if the property can not be casted to the
	 *                              specified type.
	 * @throws NullPointerException if the property has not been set.
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getProperty(String key, TypeLiteral<T> type) {
		Object value = this.properties.get(key);
		Objects.requireNonNull(value, "Missing value of property \"" + key + "\".");
		return (T) value;
	}

	/**
	 * Get an optional property value as {@link Optional}.
	 * 
	 * @param key  property name
	 * @param type expected type of the property value
	 * @return property value
	 * 
	 * @throws ClassCastException if the property can not be casted to the specified
	 *                            type.
	 */
	@SuppressWarnings("unchecked")
	protected <T> Optional<T> getOptionalProperty(String key, TypeLiteral<T> type) {
		Object value = this.properties.get(key);
		if (Objects.isNull(value)) {
			return Optional.empty();
		} else {
			return Optional.of((T) value);
		}
	}

	@Override
	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}
}
