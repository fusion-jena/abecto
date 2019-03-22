package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

/**
 * A task that returns a new {@link Graph} based on given properties using
 * {@link #setProperties(Map)}.
 */
public interface Processor extends Callable<Graph> {

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
	 * @return {@link MultiUnion}s of result data {@link Graph}s of this
	 *         {@link Processor} and its input {@link Processor}s
	 * 
	 * @see #getMetaGraph()
	 * @see #getResultGraph()
	 */
	public Map<UUID,Collection<Graph>> getDataGraphs();

	/**
	 * @return {@link MultiUnion} of result meta {@link Graph}s of this
	 *         {@link Processor} and its input {@link Processor}s
	 */
	public Collection<Graph> getMetaGraph();

	/**
	 * Returns a map of allowed properties for this processor and the required data
	 * type of the parameter
	 * 
	 * @return Map of allowed properties and the required data type.
	 */
	public Map<String, TypeLiteral<?>> getPropertyTypes();

	/**
	 * Returns the result {@link Graph} produced by this {@link Processor}. The
	 * returned {@link Graph} is either a data {@link Graph} or a meta {@link Graph}
	 * depending on the type of this {@link Processor}.
	 * 
	 * @return result {@link Graph} produced by this {@link Processor}
	 */
	public Graph getResultGraph();

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
	 * Set the properties for this processor. Earlier property configurations will
	 * be overwritten.
	 * 
	 * @param properties {@link Map} of property keys and property values.
	 */
	public void setProperties(Map<String, Object> properties);
}
