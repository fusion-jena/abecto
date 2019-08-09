package de.uni_jena.cs.fusion.abecto.configuration;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.processor.api.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class Configuration extends AbstractEntityWithUUID {

	/**
	 * The {@link KnowledgeBase} this {@link Configuration} of a
	 * {@link SourceProcessor} belongs to or {@code null}, if this does not belong
	 * to a {@link SourceProcessor}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	protected KnowledgeBase knowledgeBase;
	/**
	 * The {@link Project} this {@link Configuration} belongs to.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;

	@ManyToMany(fetch = FetchType.LAZY)
	protected Collection<Configuration> inputProcessingConfigurations = new HashSet<>();
	@OneToOne
	protected Parameter parameter;
	@SuppressWarnings("rawtypes")
	protected Class<? extends Processor> processor;

	protected Configuration() {
	}

	/**
	 * Creates a {@link Configuration} for a {@link RefinementProcessor}.
	 * 
	 * @param parameter                     The {@link Parameter} to use.
	 * @param processorClass                The {@link Processor} to use.
	 *                                      {@link SourceProcessor} are not allowed.
	 * @param inputProcessingConfigurations The {@link Configuration}s whose result
	 *                                      to use as input.
	 */
	public Configuration(Class<Processor<?>> processorClass, Parameter parameter,
			Iterable<Configuration> inputProcessingConfigurations)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(processorClass, parameter);

		// add associations between ProcessingConfigurations
		for (Configuration inputProcessingConfiguration : inputProcessingConfigurations) {
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
	 * Creates a {@link Configuration} for a {@link SourceProcessor}.
	 * 
	 * @param parameter           The {@link Parameter} to use.
	 * @param processor           The {@link SourceProcessor} to use.
	 * @param knowledgeBaseModule The {@link KnowledgeBaseModule} to assign the
	 *                            configuration to.
	 */
	public Configuration(Class<Processor<?>> processor, Parameter parameter, KnowledgeBase knowledgeBase) {
		this(processor, parameter);
		this.knowledgeBase = knowledgeBase;
		this.project = knowledgeBase.getProject();
	}

	/**
	 * Internal {@link Configuration} constructor for reuse in other constructors
	 * only.
	 * 
	 * @param parameter
	 * @param processor
	 */
	protected Configuration(Class<Processor<?>> processor, Parameter parameter) {
		this.parameter = parameter;
		this.processor = processor;
	}

	@JsonIgnore
	public void addInputProcessingConfiguration(Configuration inputProcessingConfiguration) {
		this.inputProcessingConfigurations.add(inputProcessingConfiguration);
	}

	@JsonIgnore
	public Collection<Configuration> getInputProcessingConfigurations() {
		return this.inputProcessingConfigurations;
	}

	@JsonIgnore
	public KnowledgeBase getKnowledgeBase() {
		return this.knowledgeBase;
	}

	public UUID getKnowledgeBaseId() {
		return this.knowledgeBase.getId();
	}

	public Parameter getParameter() {
		return this.parameter;
	}

	@SuppressWarnings("rawtypes")
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

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%s, project=%s]", this.id, this.project.getId());
	}

}
