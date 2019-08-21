package de.uni_jena.cs.fusion.abecto.processing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor.Status;
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

/**
 * Represents a actual execution of a processor.
 *
 */
@Entity
public class Processing extends AbstractEntityWithUUID {

	/**
	 * {@link Step} of this processing. <b>Note:</b> The {@link Step} might have
	 * been changed after processing. To get the actual {@link Step} of this
	 * processing refer to {@link #processor}, {@link #parameter}, and
	 * {@link #inputProcessings}.
	 */
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private Step step;
	/**
	 * {@link Processor} used to produce the result {@link RdfModel}.
	 */
	@SuppressWarnings("rawtypes")
	private Class<? extends Processor> processor;
	/**
	 * {@link Parameter} used to produce the result {@link RdfModel}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private Parameter parameter;
	/**
	 * Collection of {@link Processing}s used to produce the result
	 * {@link RdfModel}.
	 */
	@ManyToMany
	private Set<Processing> inputProcessings = new HashSet<>();

	// status
	private OffsetDateTime startDateTime;
	private OffsetDateTime endDateTime;
	private Status status = Status.NOT_STARTED;
	@Lob
	private String stackTrace;

	// results
	private String modelHash;

	protected Processing() {
	}

	public Processing(Step step) {
		this.step = step;
		this.parameter = step.getParameter();
		this.processor = step.getProcessorClass();
	}

	protected Processing(Step step, Collection<Processing> inputProcessings) {
		this.step = step;
		this.parameter = step.getParameter();
		this.processor = step.getProcessorClass();
		this.inputProcessings.addAll(inputProcessings);
	}

	public void addInputProcessing(Processing processing) {
		this.inputProcessings.add(processing);
	}

	@JsonIgnore
	public Step getStep() {
		return this.step;
	}

	public UUID getStepId() {
		return this.step.getId();
	}

	public OffsetDateTime getEndDateTime() {
		// TODO format JSON serialization
		return this.endDateTime;
	}

	@JsonIgnore
	public Collection<Processing> getInputProcessings() {
		return this.inputProcessings;
	}

	public Collection<UUID> getInputProcessingIds() {
		Collection<UUID> ids = new ArrayList<>();
		this.inputProcessings.forEach((p) -> ids.add(p.getId()));
		return ids;
	}

	public Parameter getParameter() {
		return this.parameter;
	}

	@SuppressWarnings("rawtypes")
	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public String getModelHash() {
		return this.modelHash;
	}

	public String getStackTrace() {
		return this.stackTrace;
	}

	public OffsetDateTime getStartDateTime() {
		// TODO format JSON serialization
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
			this.endDateTime = OffsetDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state FAILED as current state is not NOT_STARTED or RUNNING.");
		}
	}

	public Processing setStateStart() {
		if (this.isNotStarted()) {
			this.status = Status.RUNNING;
			this.startDateTime = OffsetDateTime.now();
			return this;
		} else {
			throw new IllegalStateException("Failed to set state RUNNING as current state is not NOT_STARTED.");
		}
	}

	public Processing setStateSuccess(String modelHash) {
		if (this.isRunning() || this.isNotStarted()) {
			Objects.requireNonNull(modelHash);
			this.modelHash = modelHash;
			this.status = Status.SUCCEEDED;
			this.endDateTime = OffsetDateTime.now();
			return this;
		} else {
			throw new IllegalStateException(
					"Failed to set state SUCCEEDED as current state is not NOT_STARTED or RUNNING.");
		}
	}
}
