package de.uni_jena.cs.fusion.abecto.processing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.Processor.Status;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModel;
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
	 * {@link Processor} used to produce the result {@link RdfModel}.
	 */
	private Class<? extends Processor> processor;
	/**
	 * {@link ProcessingParameter} used to produce the result {@link RdfModel}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingParameter parameter;
	/**
	 * Collection of {@link Processing}s used to produce the result
	 * {@link RdfModel}.
	 */
	@ManyToMany()
	private Set<Processing> inputProcessings = new HashSet<>();

	// status
	private ZonedDateTime startDateTime;
	private ZonedDateTime endDateTime;
	private Status status = Status.NOT_STARTED;
	@Lob
	private String stackTrace;

	// results
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private RdfModel rdfModel;

	protected Processing() {}

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

	/**
	 * @return {@link MultiUnion}s of result data {@link Model}s of this
	 *         {@link Processing} and its input {@link Processing}s
	 * 
	 * @see #getMetaModel()
	 */
	public Map<UUID, Collection<Model>> getDataModels() {
		Map<UUID, Collection<Model>> result = new HashMap<>();

		if (SourceProcessor.class.isAssignableFrom(this.processor)) {
			UUID uuid = this.configuration.getKnowledgeBase().getId();
			Collection<Model> set = new HashSet<>();
			set.add(this.getRdfModel().getModel());
			result.put(uuid, set);
		} else {
			for (Processing processing : this.inputProcessings) {
				for (Entry<UUID, Collection<Model>> entry : processing.getDataModels().entrySet()) {
					result.computeIfAbsent(entry.getKey(), (uuid) -> new HashSet<>()).addAll(entry.getValue());
				}
			}
			if (TransformationProcessor.class.isAssignableFrom(this.processor)) {
				for (Collection<Model> models : result.values()) {
					models.add(this.getRdfModel().getModel());
				}
			} else if (MetaProcessor.class.isAssignableFrom(this.processor)) {
				// do nothing
			}
		}
		return result;
	}

	public ZonedDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public Collection<Processing> getInputProcessings() {
		return this.inputProcessings;
	}

	/**
	 * @return {@link MultiUnion} of result meta {@link Model}s of this
	 *         {@link Processing} and its input {@link Processing}s
	 * 
	 * @see #getDataModels()
	 */
	public Collection<Model> getMetaModels() {
		Set<Model> result = new HashSet<>();
		for (Processing processing : this.inputProcessings) {
			result.addAll(processing.getMetaModels());
		}
		if (MetaProcessor.class.isAssignableFrom(this.processor)) {
			result.add(this.getRdfModel().getModel());
		}
		return result;
	}

	public ProcessingParameter getParameter() {
		return this.parameter;
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public Processor getProcessorInsance() throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Processor processor = this.processor.getDeclaredConstructor().newInstance();
		if (processor instanceof SourceProcessor) {
			((SourceProcessor) processor).setKnowledgBase(this.configuration.getKnowledgeBase().getId());
		}
		return processor;
	}

	public RdfModel getRdfModel() {
		return this.rdfModel;
	}

	public String getStackTrace() {
		return this.stackTrace;
	}

	public ZonedDateTime getStartDateTime() {
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
			this.endDateTime = ZonedDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state FAILED as current state is not NOT_STARTED or RUNNING.");
		}
	}

	public Processing setStateStart() {
		if (this.isNotStarted()) {
			this.status = Status.RUNNING;
			this.startDateTime = ZonedDateTime.now();
			return this;
		} else {
			throw new IllegalStateException("Failed to set state RUNNING as current state is not NOT_STARTED.");
		}
	}

	public Processing setStateSuccess(RdfModel rdfModel) {
		if (this.isRunning() || this.isNotStarted()) {
			Objects.requireNonNull(rdfModel);
			this.rdfModel = rdfModel;
			this.status = Status.SUCCEEDED;
			this.endDateTime = ZonedDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state SUCCEEDED as current state is not NOT_STARTED or RUNNING.");
		}
	}

	@Override
	public String toString() {
		return String.format(
				"Processing[id=%s, configuration=%s, processor='%s', status=%s, start=%tc, end=%tc, parameter=%s]",
				this.id, this.configuration.getId(), this.processor.getSimpleName(), this.status, this.startDateTime,
				this.endDateTime, this.parameter.getId());
	}
}
