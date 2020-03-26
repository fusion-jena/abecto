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
package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractProcessor<P extends ParameterModel> implements Processor<P> {

	private Model resultModel = Models.getEmptyOntModel();

	private Status status = Status.NOT_STARTED;
	private Throwable failureCause;

	/**
	 * The current parameters of this processor.
	 */
	private P parameters;

	@Override
	public synchronized void alert() {
		this.notifyAll();
	}

	@Override
	public synchronized void await() throws InterruptedException {
		if (this.isRunning() || this.isNotStarted()) {
			this.wait();
		}
	}

	@Override
	public Model call() throws Exception {
		this.status = Status.RUNNING;
		this.prepare();
		this.computeResultModel();
		this.status = Status.SUCCEEDED;
		this.alert();
		return resultModel;
	}

	/**
	 * Computes a {@link Model}, or throws an exception if unable to do so. This
	 * method is used by {@link #call()} and should be implemented instead of
	 * overwriting {@link #call()}.
	 * 
	 * @throws Exception
	 */
	protected abstract void computeResultModel() throws Exception;

	@Override
	public void fail(Throwable cause) throws ExecutionException {
		this.status = Status.FAILED;
		this.alert();
		throw new ExecutionException(cause);
	}

	public Throwable getFailureCause() {
		if (this.isFailed()) {
			return failureCause;
		} else {
			throw new IllegalStateException("Failed to return failure cause: Processor has not failed.");
		}
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

	protected void setModel(Model model) {
		this.resultModel = Objects.requireNonNull(model);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setParameters(ParameterModel parameters) {
		this.parameters = (P) parameters;
	}

	@Override
	public void setStatus(Status status, Model model) {
		this.status = status;
		if (status.equals(Status.SUCCEEDED)) {
			this.resultModel = Objects.requireNonNull(model);
		}
	}
}
