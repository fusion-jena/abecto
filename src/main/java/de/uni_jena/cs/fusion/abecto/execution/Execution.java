package de.uni_jena.cs.fusion.abecto.execution;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.util.EntityToIdConverter;

/**
 * Represents a actual execution of a {@link Project}.
 *
 */
@Entity
public class Execution extends AbstractEntityWithUUID {
	/**
	 * The {@link Processor}s that have been executed by this {@link Execution}.
	 */
	@ManyToMany
	@JsonSerialize(contentConverter = EntityToIdConverter.class)
	private Collection<Processing> processings;
	/**
	 * The {@link Project} that was executed by this {@link Execution}.
	 */
	@ManyToOne(optional = false)
	@JsonSerialize(converter = EntityToIdConverter.class)
	private Project project;

	/**
	 * The date time this execution was started.
	 */
	private OffsetDateTime startDateTime;

	public Execution() {
	}

	public Execution(Project project, Iterable<Processing> processings) {
		this.project = project;
		this.processings = new ArrayList<>();
		processings.forEach(this.processings::add);
		this.startDateTime = OffsetDateTime.now();
	}

	public Collection<Processing> getProcessings() {
		return processings;
	}

	public Project getProject() {
		return project;
	}

	public OffsetDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setProcessings(Collection<Processing> processings) {
		this.processings = new ArrayList<>(processings);
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void setStartDateTime(OffsetDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

}
