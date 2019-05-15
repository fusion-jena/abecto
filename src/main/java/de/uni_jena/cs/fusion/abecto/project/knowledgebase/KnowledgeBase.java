package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class KnowledgeBase extends AbstractEntityWithUUID {

	protected String label;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;

	protected KnowledgeBase() {}

	public KnowledgeBase(Project project, String label) {
		this.project = project;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonIgnore //TODO add project uuid to json
	public Project getProject() {
		return this.project;
	}

	@Override
	public String toString() {
		return String.format("KnowledgeBase[id=%s, label='%s', project=%s]", this.id, this.label, this.project.getId());
	}
}
