package de.uni_jena.cs.fusion.abecto.project.knowledgebase.module;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

@Entity
public class KnowledgeBaseModule {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long knowledgeBaseModuleId;
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

	public long getId() {
		return this.knowledgeBaseModuleId;
	}

	@Override
	public String toString() {
		return String.format("KnowledgeBaseModule[id=%d, label='%s', knowledgeBase='%s']", this.knowledgeBaseModuleId,
				this.label, this.knowledgeBase);
	}
}
