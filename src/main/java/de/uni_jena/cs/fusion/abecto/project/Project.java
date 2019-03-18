package de.uni_jena.cs.fusion.abecto.project;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

@Entity
public class Project extends AbstractEntityWithUUID {

	protected String label;
	@OneToMany(mappedBy = "project")
	protected Collection<KnowledgeBase> knowledgeBases;

	protected Project() {
	}

	public Project(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return String.format("Project[id=%s, label='%s']", this.id, this.label);
	}
}
