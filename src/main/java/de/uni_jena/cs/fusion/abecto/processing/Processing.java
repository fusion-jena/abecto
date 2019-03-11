package de.uni_jena.cs.fusion.abecto.processing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

/**
 * Represents a actual execution of a processor.
 *
 */
@Entity
public class Processing {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Lob
	private String stackTrace;
	@ManyToOne(fetch = FetchType.LAZY)
	private RdfGraph rdfGraph;
	private Class<? extends Processor> processor;
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingParameter parameter;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;
	@OneToMany()
	private Collection<Processing> inputProcessing = new HashSet<>();
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingConfiguration configuration;

	public ProcessingConfiguration getConfiguration() {
		return configuration;
	}

	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public Collection<Processing> getInputProcessing() {
		return inputProcessing;
	}

	public ProcessingParameter getParameter() {
		return parameter;
	}

	public Class<? extends Processor> getProcessor() {
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

	public void setConfiguration(ProcessingConfiguration configuration) {
		this.configuration = configuration;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endDateTime = endTime;
	}

	public void setInputProcessing(Collection<Processing> processing) {
		this.inputProcessing.addAll(processing);
	}

	public void setParameter(ProcessingParameter parameter) {
		this.parameter = parameter;
	}

	public void setProcessor(Class<? extends Processor> processor) {
		this.processor = processor;
	}

	public void setRdfGraph(RdfGraph rdfGraph) {
		Objects.requireNonNull(rdfGraph);
		this.rdfGraph = rdfGraph;
	}

	public void setStackTrace(Throwable t) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(out));
		this.stackTrace = out.toString();
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startDateTime = startTime;
	}
}
