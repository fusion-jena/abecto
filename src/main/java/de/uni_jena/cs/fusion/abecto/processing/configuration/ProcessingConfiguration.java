package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;

@Entity
public class ProcessingConfiguration {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long processingConfigurationId;
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private KnowledgeBaseModule knowledgeBaseModule;
	@ManyToMany(fetch = FetchType.LAZY)
	private Collection<ProcessingConfiguration> subsequentProcessingConfiguration;
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "subsequentProcessingConfiguration")
	private Collection<ProcessingConfiguration> precedingProcessingConfiguration;

	protected ProcessingConfiguration() {
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%d]", this.processingConfigurationId);
	}
}
