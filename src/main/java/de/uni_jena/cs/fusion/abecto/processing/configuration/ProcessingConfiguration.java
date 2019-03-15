package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.UnexpectedStateException;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.source.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.transformation.TransformationProcessor;
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
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	protected KnowledgeBase knowledgeBase;
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
		this.knowledgeBase = knowledgeBaseModule.knowledgeBase;
		this.project = knowledgeBaseModule.knowledgeBase.project;
	}

	/**
	 * 
	 * @param parameter                     The {@link ProcessingParameter} to use.
	 * @param processor                     The {@link Processor} to use.
	 *                                      {@link SourceProcessor} are not allowed.
	 * @param inputProcessingConfigurations The {@link ProcessingConfiguration}s
	 *                                      whose result to use as input.
	 * 
	 * @throws IllegalArgumentException if {@code processor} is not a
	 *                                  {@link MetaProcessor} or a
	 *                                  {@link TransformationProcessor}
	 * @throws IllegalStateException    if {@code processor} is a
	 *                                  {@link TransformationProcessor} and the
	 *                                  {@code inputProcessingConfiguration}s belong
	 *                                  to multiple {@link KnowledgeBase}
	 * @throws NoSuchElementException   if {@code processor} is a
	 *                                  {@link TransformationProcessor} and Fnone of
	 *                                  the {@code inputProcessingConfiguration}s
	 *                                  belongs to exactly {@link KnowledgeBase}
	 */
	public ProcessingConfiguration(Class<? extends TransformationProcessor> processor, ProcessingParameter parameter,
			Collection<ProcessingConfiguration> inputProcessingConfigurations)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(parameter, processor);

		if (this.isMetaProcessingConfiguration() || this.isTransformationProcessingConfiguration()) {
			// add associations between ProcessingConfigurations
			for (ProcessingConfiguration inputProcessingConfiguration : inputProcessingConfigurations) {
				this.addInputProcessingConfiguration(inputProcessingConfiguration);
			}

			this.updateProjectAssociation();
			this.updateKnowledgeBaseAssociation();
		} else {
			throw new IllegalArgumentException(
					"Argument processor needs to be a MetaProcess or a TransformationProcessor.");
		}

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

	public KnowledgeBase getKnowledgeBase() {
		return this.knowledgeBase;
	}

	public KnowledgeBaseModule getKnowledgeBaseModule() {
		return this.knowledgeBaseModule;
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
	 * @return {@code true} if this is the configuration of a {@link MetaProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isMetaProcessingConfiguration() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
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
	 * Updates the {@link KnowledgeBase} association based on the input
	 * {@link ProcessingConfiguration}s.
	 * 
	 * @return {@code true} if the {@link KnowledgeBase} association has changed,
	 *         otherwise {@code false}
	 */
	public boolean updateKnowledgeBaseAssociation() {
		boolean changed = false;

		// collect all knowledgeBase values (Note: container required to accept null)
		Set<KnowledgeBase> inputKnowledgeBases = this.inputProcessingConfigurations.stream()
				.map((configuration) -> configuration.knowledgeBase)
				.collect(HashSet::new, Set::add, (a, b) -> a.addAll(b));
		if (this.isTransformationProcessingConfiguration()) {
			if (inputKnowledgeBases.contains(null)) {
				// one inputKnowledgeBase belongs to multiple knowledge bases
				throw new IllegalStateException("Input ProcessingConfigurations belong to multiple knowledge bases.");
			} else if (inputKnowledgeBases.size() > 1) {
				// two inputKnowledgeBases belongs to different knowledge bases
				throw new IllegalStateException("Input ProcessingConfigurations belong to multiple knowledge bases.");
			} else if (inputKnowledgeBases.size() == 1) {
				// all inputKnowledgeBases belongs to one knowledge bases
				KnowledgeBase newValue = inputKnowledgeBases.iterator().next();
				changed = !newValue.equals(this.knowledgeBase);
				this.knowledgeBase = newValue;
			} else if (inputKnowledgeBases.isEmpty()) {
				throw new IllegalStateException("Input ProcessingConfigurations belong to no knowledge base.");
			} else {
				throw new UnexpectedStateException();
			}
		} else if (this.isMetaProcessingConfiguration()) {
			if (inputKnowledgeBases.contains(null)) {
				// one inputKnowledgeBase belongs to multiple knowledge bases
				changed = this.knowledgeBase != null;
				this.knowledgeBase = null;
			} else if (inputKnowledgeBases.size() > 1) {
				// two inputKnowledgeBases belongs to different knowledge bases
				changed = this.knowledgeBase != null;
				this.knowledgeBase = null;
			} else if (inputKnowledgeBases.size() == 1) {
				// all inputKnowledgeBases belongs to one knowledge bases
				KnowledgeBase newValue = inputKnowledgeBases.iterator().next();
				changed = !newValue.equals(this.knowledgeBase);
				this.knowledgeBase = newValue;
			} else if (inputKnowledgeBases.isEmpty()) {
				throw new IllegalStateException("Input ProcessingConfigurations belong to no knowledge base.");
			} else {
				throw new UnexpectedStateException();
			}
		} else if (this.isSourceProcessingConfiguration()) {
			// do nothing
		} else {
			throw new UnexpectedStateException();
		}
		return changed;
	}

	/**
	 * Updates the {@link Project} association based on the input
	 * {@link ProcessingConfiguration}s.
	 * 
	 * @return {@code true} if the {@link Project} association has changed,
	 *         otherwise {@code false}
	 */
	public boolean updateProjectAssociation() {
		boolean changed = false;

		// collect all project values
		Set<Project> inputProjects = this.inputProcessingConfigurations.stream()
				.map((configuration) -> configuration.project).collect(HashSet::new, Set::add, (a, b) -> a.addAll(b));
		if (inputProjects.size() == 1) {
			Project newValue = inputProjects.iterator().next();
			changed = !newValue.equals(this.project);
			this.project = newValue;
		} else {
			throw new IllegalStateException("Input ProcessingConfigurations belong to different project.");
		}

		return changed;
	}

}
