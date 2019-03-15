package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;

@Entity
public class ProcessingConfiguration extends AbstractEntityWithUUID {

	/**
	 * The {@link KnowledgeBaseModule} this {@link ProcessingConfiguration} belongs
	 * to. {@code null}, if it belongs to multiple {@link KnowledgeBaseModule}s.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	protected KnowledgeBaseModule knowledgeBaseModule;
	/**
	 * The {@link KnowledgeBase} this {@link ProcessingConfiguration} belongs to.
	 * {@code null}, if it belongs to multiple {@link KnowledgeBase}s.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	protected Collection<KnowledgeBase> knowledgeBases = new HashSet<>();
	/**
	 * The {@link Project} this {@link ProcessingConfiguration} belongs to.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "inputProcessingConfigurations")
	protected Collection<ProcessingConfiguration> subsequentProcessingConfigurations = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	protected Collection<ProcessingConfiguration> inputProcessingConfigurations = new HashSet<>();
	@ManyToOne(fetch = FetchType.LAZY)
	protected ProcessingParameter parameter;
	protected Class<? extends Processor> processor;

	protected LocalDateTime lastChange;

	protected ProcessingConfiguration() {
	}

	/**
	 * @param parameter                     The {@link ProcessingParameter} to use.
	 * @param processor                     The {@link Processor} to use.
	 *                                      {@link SourceProcessor} are not allowed.
	 * @param inputProcessingConfigurations The {@link ProcessingConfiguration}s
	 *                                      whose result to use as input.
	 */
	public ProcessingConfiguration(Class<? extends RefinementProcessor> processor, ProcessingParameter parameter,
			Collection<ProcessingConfiguration> inputProcessingConfigurations)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(parameter, processor);

		// add associations between ProcessingConfigurations
		for (ProcessingConfiguration inputProcessingConfiguration : inputProcessingConfigurations) {
			this.addInputProcessingConfiguration(inputProcessingConfiguration);
		}

		this.updateAssociations();
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
			KnowledgeBaseModule knowledgeBaseModule) {
		this(parameter, processor);
		this.knowledgeBaseModule = knowledgeBaseModule;
		this.knowledgeBases.add(knowledgeBaseModule.knowledgeBase);
		this.project = knowledgeBaseModule.knowledgeBase.project;
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

	public void addInputProcessingConfiguration(ProcessingConfiguration inputProcessingConfiguration) {
		this.inputProcessingConfigurations.add(inputProcessingConfiguration);
		inputProcessingConfiguration.subsequentProcessingConfigurations.add(this);
	}

	public Collection<ProcessingConfiguration> getInputProcessingConfigurations() {
		return this.inputProcessingConfigurations;
	}

	public KnowledgeBaseModule getKnowledgeBaseModule() {
		return this.knowledgeBaseModule;
	}

	public Collection<KnowledgeBase> getKnowledgeBases() {
		return Collections.unmodifiableCollection(this.knowledgeBases);
	}

	public LocalDateTime getLastChange() {
		return this.lastChange;
	}

	public ProcessingParameter getParameter() {
		return this.parameter;
	}

	public Processor getProcessor() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		return this.getProcessorClass().getDeclaredConstructor().newInstance();
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public Project getProject() {
		return this.project;
	}

	public Collection<ProcessingConfiguration> getSubsequentProcessingConfigurations() {
		return this.subsequentProcessingConfigurations;
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link MappingProcessor}, otherwise {@code false}
	 */
	public boolean isMappingProcessingConfiguration() {
		return MappingProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a {@link MetaProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isMetaProcessingConfiguration() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link RefinementProcessor}, otherwise {@code false}
	 */
	public boolean isRefinementProcessingConfiguration() {
		return RefinementProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link SourceProcessor}, otherwise {@code false}
	 */
	public boolean isSourceProcessingConfiguration() {
		return SourceProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if this is the configuration of a
	 *         {@link TransformationProcessor}, otherwise {@code false}
	 */
	public boolean isTransformationProcessingConfiguration() {
		return TransformationProcessor.class.isAssignableFrom(this.processor);
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%s]", this.id);
	}

	/**
	 * Updates the {@link KnowledgeBase}s and {@link Project} associations based on
	 * the input {@link ProcessingConfiguration}s.
	 * 
	 * @throws IllegalStateException if this {@link ProcessingConfiguration} belongs
	 *                               to multiple projects
	 */
	public void updateAssociations() throws IllegalStateException {
		// update knowledgeBaseModule ((Note: container required to accept null)
		Set<KnowledgeBaseModule> inputKnowledgeBaseModules = this.inputProcessingConfigurations.stream()
				.map((configuration) -> configuration.knowledgeBaseModule)
				.collect(HashSet::new, Set::add, (a, b) -> a.addAll(b));
		if (inputKnowledgeBaseModules.size() == 1) {
			this.knowledgeBaseModule = inputKnowledgeBaseModules.iterator().next();
		} else {
			inputKnowledgeBaseModules = null;
		}

		// update knowledgeBases
		this.knowledgeBases = this.inputProcessingConfigurations.stream()
				.map((configuration) -> configuration.knowledgeBases)
				.collect(HashSet::new, Set::addAll, (a, b) -> a.addAll(b));

		// update project
		Set<Project> inputProjects = this.inputProcessingConfigurations.stream()
				.map((configuration) -> configuration.project).collect(HashSet::new, Set::add, (a, b) -> a.addAll(b));
		if (inputProjects.size() == 1) {
			this.project = inputProjects.iterator().next();
		} else {
			throw new IllegalStateException(String.format("%s belongs to multiple project.", this));
		}
	}

}
