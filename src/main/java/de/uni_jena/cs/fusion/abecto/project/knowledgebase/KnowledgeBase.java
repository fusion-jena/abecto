package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;

@Entity
public class KnowledgeBase extends AbstractEntityWithUUID {

	protected String label;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;
	@OneToMany(mappedBy = "knowledgeBase")
	protected Collection<KnowledgeBaseModule> knowledgeBaseModules;

	protected KnowledgeBase() {
	}

	public KnowledgeBase(Project project, String label) {
		this.project = project;
		this.label = label;
	}

	@Override
	public String toString() {
		return String.format("KnowledgeBase[id=%s, label='%s', project='%s']", this.id, this.label,
				this.project);
	}
	
	public Project getProject() {
		return this.project;
	}
}
