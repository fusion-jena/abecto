package de.uni_jena.cs.fusion.abecto.processor.api;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.rdf.model.Model;
import org.springframework.core.GenericTypeResolver;

/**
 * A task that returns a new {@link Model} based on given properties using
 * {@link #setParameters(Map)}.
 * 
 * @param <P> Parameter Model Type
 */
public interface Processor<P extends ProcessorParameters> extends Callable<Model> {

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
	 * Set this {@link Processor} to status failed.
	 */
	public void fail();

	/**
	 * @return {@link MultiUnion}s of result data {@link Model}s of this
	 *         {@link Processor} and its input {@link Processor}s
	 * 
	 * @see #getMetaModel()
	 * @see #getResultModel()
	 */
	public Map<UUID, Collection<Model>> getDataModels();

	/**
	 * @param processorClass {@link Processor} implementation to determine the
	 *                       {@link ProcessorParameters} class for.
	 * @return {@link ProcessorParameters} class belonging to the given
	 *         {@link Processor} implementation.
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends ProcessorParameters> getParameterClass(Class<? extends Processor<?>> processorClass) {
		return (Class<? extends ProcessorParameters>) GenericTypeResolver.resolveTypeArgument(processorClass,
				Processor.class);
	}

	/**
	 * 
	 * @param processorClass {@link Processor} implementation to instantiate
	 *                       {@link ProcessorParameters} for.
	 * @return {@link ProcessorParameters} instance with default values for a given
	 *         {@link Processor} implementation.
	 * @throws InstantiationException if the processor is abstract.
	 * @throws IllegalAccessException if the processor constructor without parameter is not accessible.
	 * @throws InvocationTargetException if the processor constructor throws an exception.
	 * @throws NoSuchMethodException if the processor constructor without parameter is not available.
	 * @throws SecurityException
	 */
	public static ProcessorParameters getDefaultParameters(Class<? extends Processor<?>> processorClass)
			throws InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		return (ProcessorParameters) getParameterClass(processorClass).getConstructor(new Class[0])
				.newInstance(new Object[0]);
	}

	/**
	 * @return {@link MultiUnion} of result meta {@link Model}s of this
	 *         {@link Processor} and its input {@link Processor}s
	 */
	public Collection<Model> getMetaModel();

	/**
	 * Returns the parameters of this processor.
	 * 
	 * @return Parameters of this processor.
	 */
	public P getParameters();

	/**
	 * Returns the result {@link Model} produced by this {@link Processor}. The
	 * returned {@link Model} is either a data {@link Model} or a meta {@link Model}
	 * depending on the type of this {@link Processor}.
	 * 
	 * @return result {@link Model} produced by this {@link Processor}
	 */
	public Model getResultModel();

	/**
	 * @return {@link Status} of this {@link Processor}
	 */
	public Status getStatus();

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
	 * Set the parameters for this processor. Earlier parameter configurations will
	 * be overwritten.
	 * 
	 * @param parameters TODO
	 */
	public void setParameters(ProcessorParameters parameters);

	/**
	 * Set the result {@link Model} for this {@link Processor}.
	 * 
	 * @param resultModel {@link Model} to use as result model
	 */
	public void setResultModel(Model model);

	/**
	 * Set the {@link Status} for this {@link Processor}.
	 * 
	 * @param resultModel {@link Status} to set
	 */
	public void setStatus(Status status);
}
