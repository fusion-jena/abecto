package de.uni_jena.cs.fusion.abecto.processing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.processor.transformation.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

/**
 * Represents a actual execution of a processor.
 *
 */
@Entity
public class Processing extends AbstractEntityWithUUID {

	private enum ProcessingStatus {
		NOT_STARTED, RUNNING, SUCCEEDED, FAILED
	}

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
	private ProcessingStatus status = ProcessingStatus.NOT_STARTED;
	@Lob
	private String stackTrace;

	// results
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private RdfGraph rdfGraph;

	public Processing(ProcessingConfiguration configuration, Collection<Processing> inputProcessings) {
		this.configuration = configuration;
		this.parameter = configuration.getParameter();
		this.processor = configuration.getProcessorClass();
		this.inputProcessings.addAll(inputProcessings);
	}

	public ProcessingConfiguration getConfiguration() {
		return configuration;
	}

	public Processor getConfiguredProcessor(ProgressListener listener)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

		Processor processor = this.processor.getDeclaredConstructor().newInstance();

		processor.setListener(listener);
		processor.setProperties(this.parameter.getAll());
		if (processor instanceof TransformationProcessor) {
			Collection<RdfGraph> inputGraphs = this.inputProcessings.stream().map(Processing::getRdfGraph)
					.collect(Collectors.toSet());
			((TransformationProcessor) processor).setInputGraphs(inputGraphs);
		}

		return processor;
	}

	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public Collection<Processing> getInputProcessing() {
		return inputProcessings;
	}

	public ProcessingParameter getParameter() {
		return parameter;
	}

	public Class<? extends Processor> getProcessorClass() {
		return processor;
	}

	public RdfGraph getRdfGraph() {
		return this.rdfGraph;
	}

	public String getStackTrace() {
		return this.stackTrace;
	}

	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public ProcessingStatus getStatus() {
		return status;
	}

	public Processing setStateFail(Throwable t) {
		if (this.status == ProcessingStatus.RUNNING) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			t.printStackTrace(new PrintStream(out));
			this.stackTrace = out.toString();
			this.endDateTime = LocalDateTime.now();
			this.status = ProcessingStatus.FAILED;
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set processing state \"failed\". Processing is not in state \"running\".");
		}
	}

	public Processing setStateStart() {
		if (this.status == ProcessingStatus.NOT_STARTED) {
			this.status = ProcessingStatus.RUNNING;
			this.startDateTime = LocalDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set processing state \"running\". Processing is not in state \"not started\".");
		}
	}

	public Processing setStateSuccess(RdfGraph rdfGraph) {
		if (this.status == ProcessingStatus.RUNNING) {
			Objects.requireNonNull(rdfGraph);
			this.rdfGraph = rdfGraph;
			this.status = ProcessingStatus.SUCCEEDED;
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set processing state \"succeeded\". Processing is not in state \"running\".");
		}
	}
}
