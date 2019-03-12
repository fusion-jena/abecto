package de.uni_jena.cs.fusion.abecto.project.knowledgebase.module;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

@Entity
public class KnowledgeBaseModule extends AbstractEntityWithUUID {

	public String label;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	public KnowledgeBase knowledgeBase;
	@OneToMany(mappedBy = "knowledgeBaseModule")
	public Collection<ProcessingConfiguration> sourceProcessingConfigurations;

	protected KnowledgeBaseModule() {
	}

	public KnowledgeBaseModule(KnowledgeBase knowledgeBase, String label) {
		this.knowledgeBase = knowledgeBase;
		this.label = label;
	}

	@Override
	public String toString() {
		return String.format("KnowledgeBaseModule[id=%s, label='%s', knowledgeBase='%s']", this.id, this.label,
				this.knowledgeBase);
	}
}
