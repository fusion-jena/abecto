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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.springframework.core.GenericTypeResolver;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

/**
 * Provides an interface for a task that outputs a new {@link Model} based on
 * given input {@link Model}s and parameters provided with by an
 * {@link ParameterModel} object. A {@link Processor} implementation is linked
 * to the appropriate {@link ParameterModel} using the type parameter {@link P}.
 * 
 * @param <P> Parameter Model Type
 */
public interface Processor<P extends ParameterModel> extends Callable<Model> {

	/**
	 * {@link Status} of the {@link Processor}
	 */
	public enum Status {
		NOT_STARTED, RUNNING, SUCCEEDED, FAILED
	}

	/**
	 * Notify waiting {@link Processor}s.
	 * 
	 * @see #await()
	 */
	public void alert();

	/**
	 * Wait for the notification by this {@link Processor}.
	 * 
	 * @throws InterruptedException
	 * 
	 * @see #alert()
	 */
	public void await() throws InterruptedException;

	/**
	 * Sets this {@link Processor} to status failed and throws an
	 * {@link ExecutionException}.
	 * 
	 * @throws ExecutionException if this method is called
	 */
	public void fail(Throwable cause) throws ExecutionException;

	/**
	 * @return Result data {@link Model}s of this {@link Processor} and its input
	 *         {@link Processor}s
	 * 
	 * @see #getMetaModels()
	 * @see #getResultModel()
	 */
	public Map<UUID, Collection<Model>> getDataModels();

	/**
	 * @return the cause of failure of this {@link Processor}
	 * @throws IllegalStateException if this {@link Processor} is not failed
	 */
	public Throwable getFailureCause();

	/**
	 * @param processorClass {@link Processor} implementation to determine the
	 *                       {@link ParameterModel} class for.
	 * @return {@link ParameterModel} class belonging to the given {@link Processor}
	 *         implementation.
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends ParameterModel> getParameterClass(Class<? extends Processor<?>> processorClass) {
		return (Class<? extends ParameterModel>) GenericTypeResolver.resolveTypeArgument(processorClass,
				Processor.class);
	}

	/**
	 * 
	 * @param processorClass {@link Processor} implementation to instantiate
	 *                       {@link ParameterModel} for.
	 * @return {@link ParameterModel} instance with default values for a given
	 *         {@link Processor} implementation.
	 * @throws InstantiationException    if the processor is abstract.
	 * @throws IllegalAccessException    if the processor constructor without
	 *                                   parameter is not accessible.
	 * @throws InvocationTargetException if the processor constructor throws an
	 *                                   exception.
	 * @throws NoSuchMethodException     if the processor constructor without
	 *                                   parameter is not available.
	 * @throws SecurityException
	 */
	public static ParameterModel getDefaultParameters(Class<? extends Processor<?>> processorClass)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			SecurityException {
		return (ParameterModel) getParameterClass(processorClass).getConstructor(new Class[0])
				.newInstance(new Object[0]);
	}

	/**
	 * @return Result meta {@link Model}s of this {@link Processor} and its input
	 *         {@link Processor}s
	 */
	public Collection<Model> getMetaModels();

	/**
	 * Returns the parameters of this processor.
	 * 
	 * @return Parameters of this processor.
	 */
	public P getParameters();

	/**
	 * Returns the result {@link Model} produced by this {@link Processor}. The
	 * returned {@link Model} is either a data {@link Model} or a meta {@link Model}
	 * depending on the type of this {@link Processor}. If called before the
	 * calculation has succeeded, an empty or intermediate model will be returned.
	 * 
	 * @return result the result produced by this {@link Processor}
	 */
	public Model getResultModel();

	/**
	 * @return the status of this {@link Processor}
	 */
	public Status getStatus();

	/**
	 * Provides the identifier of the ontology, this {@link Processor} belongs
	 * to. <strong>Using this method restricts the Processor to work on exact one
	 * ontology.</strong>
	 * 
	 * @return the ontology identifier
	 */
	public UUID getOntology();

	/**
	 * 
	 * @return {@code true} if this {@link Processor} has {@link Status#FAILED},
	 *         else {@code false}
	 */
	public boolean isFailed();

	/**
	 * 
	 * @return {@code true} if this {@link Processor} has
	 *         {@link Status#NOT_STARTED}, else {@code false}
	 */
	public boolean isNotStarted();

	/**
	 * 
	 * @return {@code true} if this {@link Processor} has {@link Status#RUNNING},
	 *         else {@code false}
	 */
	public boolean isRunning();

	/**
	 * 
	 * @return {@code true} if this {@link Processor} has {@link Status#SUCCEEDED},
	 *         else {@code false}
	 */
	public boolean isSucceeded();

	/**
	 * Sets the parameters for this processor. Earlier parameters will be
	 * overwritten.
	 * 
	 * @param parameters the parameters to set
	 */
	public void setParameters(ParameterModel parameters);

	/**
	 * Sets the {@link Status} and result {@link Model} for this {@link Processor}.
	 * 
	 * @param status the status of this processor
	 * @param model  the result model of this processor, if this processor is
	 *               succeeded, otherwise null is permitted
	 */
	public void setStatus(Status status, Model model);
}
