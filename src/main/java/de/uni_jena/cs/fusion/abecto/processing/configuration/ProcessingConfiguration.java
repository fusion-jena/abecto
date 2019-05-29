package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class ProcessingConfiguration extends AbstractEntityWithUUID {

	/**
	 * The {@link KnowledgeBase} this {@link ProcessingConfiguration} of a
	 * {@link SourceProcessor} belongs to or {@code null}, if this does not belong
	 * toa {@link SourceProcessor}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	protected KnowledgeBase knowledgeBase;
	/**
	 * The {@link Project} this {@link ProcessingConfiguration} belongs to.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;

	@ManyToMany(fetch = FetchType.LAZY)
	protected Collection<ProcessingConfiguration> inputProcessingConfigurations = new HashSet<>();
	@ManyToOne(fetch = FetchType.LAZY)
	protected ProcessingParameter parameter;
	protected Class<? extends Processor> processor;

	protected LocalDateTime lastChange;

	protected ProcessingConfiguration() {}

	/**
	 * @param parameter                     The {@link ProcessingParameter} to use.
	 * @param processor                     The {@link Processor} to use.
	 *                                      {@link SourceProcessor} are not allowed.
	 * @param inputProcessingConfigurations The {@link ProcessingConfiguration}s
	 *                                      whose result to use as input.
	 */
	public ProcessingConfiguration(Class<? extends RefinementProcessor> processor, ProcessingParameter parameter,
			Iterable<ProcessingConfiguration> inputProcessingConfigurations)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(parameter, processor);

		// add associations between ProcessingConfigurations
		for (ProcessingConfiguration inputProcessingConfiguration : inputProcessingConfigurations) {
			this.addInputProcessingConfiguration(inputProcessingConfiguration);
		}

		// set project
		if (this.inputProcessingConfigurations.stream().map((configuration) -> configuration.project).distinct()
				.count() == 1L) {
			this.project = this.inputProcessingConfigurations.iterator().next().project;
		} else {
			throw new IllegalStateException("InputProcessingConfigurations belong to multiple projects.");
		}
	}

	/**
	 * Creates a {@link ProcessingConfiguration} for a {@link SourceProcessor}.
	 * 
	 * @param parameter           The {@link ProcessingParameter} to use.
	 * @param processor           The {@link SourceProcessor} to use.
	 * @param knowledgeBaseModule The {@link KnowledgeBaseModule} to assign the
	 *                            configuration to.
	 */
	public ProcessingConfiguration(Class<? extends SourceProcessor> processor, ProcessingParameter parameter,
			KnowledgeBase knowledgeBase) {
		this(parameter, processor);
		this.knowledgeBase = knowledgeBase;
		this.project = knowledgeBase.getProject();
	}

	/**
	 * Internal {@link ProcessingConfiguration} constructor for reuse in other
	 * constructors only.
	 * 
	 * @param parameter
	 * @param processor
	 */
	protected ProcessingConfiguration(ProcessingParameter parameter, Class<? extends Processor> processor) {
		this.parameter = parameter;
		this.processor = processor;
		this.lastChange = LocalDateTime.now();
	}

	@JsonIgnore
	public void addInputProcessingConfiguration(ProcessingConfiguration inputProcessingConfiguration) {
		this.inputProcessingConfigurations.add(inputProcessingConfiguration);
	}
	
	@JsonIgnore
	public Collection<ProcessingConfiguration> getInputProcessingConfigurations() {
		return this.inputProcessingConfigurations;
	}

	@JsonIgnore
	public KnowledgeBase getKnowledgeBase() {
		return this.knowledgeBase;
	}

	public UUID getKnowledgeBaseId() {
		return this.knowledgeBase.getId();
	}

	@JsonIgnore
	public LocalDateTime getLastChange() {
		return this.lastChange;
	}

	@JsonIgnore
	public ProcessingParameter getProcessingParameter() {
		return this.parameter;
	}

	public Map<String, Object> getParameter() {
		return this.parameter.getMap();
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	@JsonIgnore
	public Project getProject() {
		return this.project;
	}

	public UUID getProjectId() {
		return this.project.getId();
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link MappingProcessor}, otherwise {@code false}
	 */
	public boolean isMapping() {
		return MappingProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a {@link MetaProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isMeta() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link RefinementProcessor}, otherwise {@code false}
	 */
	public boolean isRefinement() {
		return RefinementProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link SourceProcessor}, otherwise {@code false}
	 */
	public boolean isSource() {
		return SourceProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link TransformationProcessor}, otherwise {@code false}
	 */
	public boolean isTransformation() {
		return TransformationProcessor.class.isAssignableFrom(this.processor);
	}

	public void setParameter(ProcessingParameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%s, project=%s]", this.id, this.project.getId());
	}

}
