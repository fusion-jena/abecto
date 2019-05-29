package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import java.util.Collection;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class KnowledgeBase extends AbstractEntityWithUUID {

	protected String label;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;

	@OneToMany(mappedBy = "knowledgeBase", cascade = CascadeType.REMOVE)
	protected Collection<ProcessingConfiguration> sources;

	protected KnowledgeBase() {}

	public KnowledgeBase(Project project, String label) {
		this.project = project;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonIgnore
	public Project getProject() {
		return this.project;
	}

	public UUID getProjectId() {
		return this.project.getId();
	}

	@Override
	public String toString() {
		return String.format("KnowledgeBase[id=%s, label='%s', project=%s]", this.id, this.label, this.project.getId());
	}
}
