package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;

@Entity
public class ProcessingConfiguration {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long processingConfigurationId;
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private KnowledgeBaseModule knowledgeBaseModule;
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "inputProcessingConfigurations")
	private Collection<ProcessingConfiguration> subsequentProcessingConfigurations = new HashSet<>();
	@ManyToMany(fetch = FetchType.LAZY)
	private Collection<ProcessingConfiguration> inputProcessingConfigurations = new HashSet<>();
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingParameter parameter;

	private Class<? extends Processor> processor;

	protected ProcessingConfiguration() {
	}

	public ProcessingConfiguration(ProcessingParameter parameter, Class<? extends Processor> processor,
			Collection<ProcessingConfiguration> inputProcessingConfigurations) {
		this.parameter = parameter;
		this.processor = processor;
		this.inputProcessingConfigurations.addAll(inputProcessingConfigurations);
		for (ProcessingConfiguration inputProcessingConfiguration : inputProcessingConfigurations) {
			inputProcessingConfiguration.subsequentProcessingConfigurations.add(this);
		}
	}

	public ProcessingConfiguration(ProcessingParameter parameter, Class<? extends Processor> processor,
			KnowledgeBaseModule knowledgeBaseModule) {
		this.parameter = parameter;
		this.processor = processor;
		this.knowledgeBaseModule = knowledgeBaseModule;
	}

	public Collection<ProcessingConfiguration> getInputProcessingConfigurations() {
		return inputProcessingConfigurations;
	}

	public KnowledgeBaseModule getKnowledgeBaseModule() {
		return knowledgeBaseModule;
	}

	public ProcessingParameter getParameter() {
		return parameter;
	}

	public Long getProcessingConfigurationId() {
		return processingConfigurationId;
	}

	public Processor getProcessor() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return this.processor.getDeclaredConstructor().newInstance();
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public Collection<ProcessingConfiguration> getSubsequentProcessingConfigurations() {
		return subsequentProcessingConfigurations;
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%d]", this.processingConfigurationId);
	}
}
