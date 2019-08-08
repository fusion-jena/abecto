package de.uni_jena.cs.fusion.abecto.processor.api;

import java.util.Objects;

import org.apache.jena.rdf.model.Model;

public abstract class AbstractProcessor<P extends ParameterModel> implements Processor<P> {

	private Model resultModel;

	private Status status = Status.NOT_STARTED;

	/**
	 * The current configuration parameters of this processor.
	 */
	private P parameters;

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

	@Override
	public P getParameters() {
		return this.parameters;
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
	@SuppressWarnings("unchecked")
	public void setParameters(ParameterModel parameters) {
		this.parameters = (P) parameters;
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
		return String.format("%s%s", this.getClass().getSimpleName(), this.parameters);
	}
}
