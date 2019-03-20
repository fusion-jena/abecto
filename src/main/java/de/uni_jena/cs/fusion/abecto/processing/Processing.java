package de.uni_jena.cs.fusion.abecto.processing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.Processor.Status;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

/**
 * Represents a actual execution of a processor.
 *
 */
@Entity
public class Processing extends AbstractEntityWithUUID {

	// configuration
	/**
	 * {@link ProcessingConfiguration} of this processing. <b>Note:</b> The
	 * configuration might have been changed after processing. To get the actual
	 * configuration of this processing refer to {@link #processor},
	 * {@link #parameter}, and {@link #inputProcessings}.
	 */
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private ProcessingConfiguration configuration;
	/**
	 * {@link Processor} used to produce the result {@link RdfGraph}.
	 */
	private Class<? extends Processor> processor;
	/**
	 * {@link ProcessingParameter} used to produce the result {@link RdfGraph}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingParameter parameter;
	/**
	 * Collection of {@link Processing}s used to produce the result
	 * {@link RdfGraph}.
	 */
	@ManyToMany()
	private Set<Processing> inputProcessings = new HashSet<>();

	// status
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;
	private Status status = Status.NOT_STARTED;
	@Lob
	private String stackTrace;

	// results
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private RdfGraph rdfGraph;

	public Processing(ProcessingConfiguration configuration) {
		this.configuration = configuration;
		this.parameter = configuration.getParameter();
		this.processor = configuration.getProcessorClass();
	}

	protected Processing(ProcessingConfiguration configuration, Collection<Processing> inputProcessings) {
		this.configuration = configuration;
		this.parameter = configuration.getParameter();
		this.processor = configuration.getProcessorClass();
		this.inputProcessings.addAll(inputProcessings);
	}

	public ProcessingConfiguration getConfiguration() {
		return this.configuration;
	}

	public Processor getProcessorInsance() throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return this.processor.getDeclaredConstructor().newInstance();
	}

	public LocalDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public Collection<Processing> getInputProcessings() {
		return this.inputProcessings;
	}

	public ProcessingParameter getParameter() {
		return this.parameter;
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public RdfGraph getRdfGraph() {
		return this.rdfGraph;
	}

	public String getStackTrace() {
		return this.stackTrace;
	}

	public LocalDateTime getStartDateTime() {
		return this.startDateTime;
	}

	/**
	 * @return {@link Status} of this {@link Processing}
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * 
	 * @return {@code true} if this {@link Processing} has {@link Status#FAILED},
	 *         else {@code false}
	 */
	public boolean isFailed() {
		return Status.FAILED.equals(this.status);
	}

	/**
	 * 
	 * @return {@code true} if this {@link Processing} has
	 *         {@link Status#NOT_STARTED}, else {@code false}
	 */
	public boolean isNotStarted() {
		return Status.NOT_STARTED.equals(this.status);
	}

	/**
	 * 
	 * @return {@code true} if this {@link Processing} has {@link Status#RUNNING},
	 *         else {@code false}
	 */
	public boolean isRunning() {
		return Status.RUNNING.equals(this.status);
	}

	/**
	 * 
	 * @return {@code true} if this {@link Processing} has {@link Status#SUCCEEDED},
	 *         else {@code false}
	 */
	public boolean isSucceeded() {
		return Status.SUCCEEDED.equals(this.status);
	}

	public Processing setStateFail(Throwable t) {
		if (this.isRunning() || this.isNotStarted()) {
			this.status = Status.FAILED;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			t.printStackTrace(new PrintStream(out));
			this.stackTrace = out.toString();
			this.endDateTime = LocalDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state FAILED as current state is not NOT_STARTED or RUNNING.");
		}
	}

	public Processing setStateStart() {
		if (this.isNotStarted()) {
			this.status = Status.RUNNING;
			this.startDateTime = LocalDateTime.now();
			return this;
		} else {
			throw new IllegalStateException("Failed to set state RUNNING as current state is not NOT_STARTED.");
		}
	}

	public Processing setStateSuccess(RdfGraph rdfGraph) {
		if (this.isRunning() || this.isNotStarted()) {
			Objects.requireNonNull(rdfGraph);
			this.rdfGraph = rdfGraph;
			this.status = Status.SUCCEEDED;
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state SUCCEEDED as current state is not NOT_STARTED or RUNNING.");
		}
	}

	/**
	 * @return {@link MultiUnion}s of result data {@link Graph}s of this
	 *         {@link Processing} and its input {@link Processing}s
	 * 
	 * @see #getMetaGraph()
	 * @see #getResultGraph()
	 */
	public Collection<Graph> getDataGraphs() {
		// TODO Processing#getDataGraphs
		return null;
	}

	/**
	 * @return {@link MultiUnion} of result meta {@link Graph}s of this
	 *         {@link Processing} and its input {@link Processing}s
	 */
	public Graph getMetaGraph() {
		// TODO Processing#getMetaGraph
		return null;
	}
}
