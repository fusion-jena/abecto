package de.uni_jena.cs.fusion.abecto.project;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

@Entity
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long projectId;
	public String label;
	@OneToMany(mappedBy = "project")
	public Collection<KnowledgeBase> knowledgeBases;

	protected Project() {
	}

	public Project(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return String.format("Project[id=%d, label='%s']", this.projectId, this.label);
	}
}
