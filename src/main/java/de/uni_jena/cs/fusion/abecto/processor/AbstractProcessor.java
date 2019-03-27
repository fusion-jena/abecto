package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;

public abstract class AbstractProcessor implements Processor {

	private Model resultModel;

	private Status status = Status.NOT_STARTED;

	/**
	 * <p>
	 * The current configuration properties of this processor.
	 * </p>
	 * 
	 * <p>
	 * Use {@link #getProperty(String, Class)},
	 * {@link #getProperty(String, TypeLiteral, Function)},
	 * {@link #getOptionalProperty(String, Class)} or
	 * {@link #getOptionalProperty(String, TypeLiteral, Function) for type safe
	 * access without need of missing error exception handling.
	 * </p>
	 */
	private Map<String, Object> properties;

	@Override
	public synchronized void alert() {
		this.notifyAll();
	}

	@Override
	public synchronized void await() throws InterruptedException {
		this.wait();
	}

	@Override
	public Model call() throws Exception {
		this.status = Status.RUNNING;
		this.prepare();
		this.resultModel = this.computeResultModel();
		this.status = Status.SUCCEEDED;
		this.alert();
		return resultModel;
	}

	/**
	 * <p>
	 * Computes a {@link Model}, or throws an exception if unable to do so. This
	 * method is used by {@link #call()} should be implemented instead of
	 * overwriting {@link #call()}.
	 * </p>
	 * 
	 * @return computed {@link Model}
	 * @throws Exception
	 */
	protected abstract Model computeResultModel() throws Exception;

	@Override
	public void fail() {
		this.status = Status.FAILED;
		this.alert();
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

	/**
	 * Get an optional property value as {@link Optional}.
	 * 
	 * @param key       property name
	 * @param type      expected type of the property value
	 * @param converter
	 * @return property value
	 * 
	 * @throws ClassCastException if the property can not be casted to the specified
	 *                            type.
	 */
	@SuppressWarnings("unchecked")
	protected <T, R> Optional<R> getOptionalProperty(String key, TypeLiteral<T> type, Function<T, R> converter) {
		Object value = this.properties.get(key);
		if (Objects.isNull(value)) {
			return Optional.empty();
		} else {
			return Optional.of(converter.apply((T) value));
		}
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
	 * 
	 * Get an property value.
	 * 
	 * @param key       property name
	 * @param type      expected type of the property value
	 * @param converter
	 * @return property value
	 * 
	 * @throws ClassCastException   if the property can not be casted to the
	 *                              specified type.
	 * @throws NullPointerException if the property has not been set.
	 */
	@SuppressWarnings("unchecked")
	protected <T, R> R getProperty(String key, TypeLiteral<T> type, Function<T, R> converter) {
		Object value = this.properties.get(key);
		Objects.requireNonNull(value, "Missing value of property \"" + key + "\".");
		return converter.apply((T) value);
	}

	@Override
	public Model getResultModel() {
		return this.resultModel;
	}

	@Override
	public Status getStatus() {
		return this.status;
	}

	@Override
	public boolean isFailed() {
		return Status.FAILED.equals(this.status);
	}

	@Override
	public boolean isNotStarted() {
		return Status.NOT_STARTED.equals(this.status);
	}

	@Override
	public boolean isRunning() {
		return Status.RUNNING.equals(this.status);
	}

	@Override
	public boolean isSucceeded() {
		return Status.SUCCEEDED.equals(this.status);
	}

	/**
	 * Prepares this {@link Processor} for the computation. If dependencies are not
	 * available so far it will wait for a notification.
	 * 
	 * @throws Exception
	 */
	protected abstract void prepare() throws Exception;

	@Override
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public void setResultModel(Model model) {
		this.resultModel = Objects.requireNonNull(model);
	}

	@Override
	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return String.format("%s%s", this.getClass().getSimpleName(), this.properties);
	}
}
