package de.uni_jena.cs.fusion.abecto.processing.configuration;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import de.uni_jena.cs.fusion.abecto.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processor.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.source.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.transformation.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;

@Entity
public class ProcessingConfiguration {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long processingConfigurationId;

	/**
	 * The {@link KnowledgeBaseModule} this {@link ProcessingConfiguration} belongs
	 * to. {@code null}, if it belongs to multiple {@link KnowledgeBaseModule}s.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private KnowledgeBaseModule knowledgeBaseModule;

	/**
	 * The {@link KnowledgeBase} this {@link ProcessingConfiguration} belongs to.
	 * {@code null}, if it belongs to multiple {@link KnowledgeBase}s.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private KnowledgeBase knowledgeBase;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "inputProcessingConfigurations")
	private Collection<ProcessingConfiguration> subsequentProcessingConfigurations = new HashSet<>();
	@ManyToMany(fetch = FetchType.LAZY)
	private Collection<ProcessingConfiguration> inputProcessingConfigurations = new HashSet<>();
	@ManyToOne(fetch = FetchType.LAZY)
	private ProcessingParameter parameter;

	private Class<? extends Processor> processor;

	private LocalDateTime lastChange;

	protected ProcessingConfiguration() {
	}

	/**
	 * Internal {@link ProcessingConfiguration} constructor for reuse in other
	 * constructors only.
	 * 
	 * @param parameter
	 * @param processor
	 */
	private ProcessingConfiguration(ProcessingParameter parameter, Class<? extends Processor> processor) {
		this.parameter = parameter;
		this.processor = processor;
		this.lastChange = LocalDateTime.now();
	}

	/**
	 * 
	 * @param parameter                     The {@link ProcessingParameter} to use.
	 * @param processor                     The {@link Processor} to use.
	 *                                      {@link SourceProcessor} are not allowed.
	 * @param inputProcessingConfigurations The {@link ProcessingConfiguration}s
	 *                                      whose result to use as input.
	 * 
	 * @throws IllegalArgumentException if {@code processor} is a
	 *                                  {@link SourceProcessor}
	 * @throws IllegalStateException    if {@code processor} is a
	 *                                  {@link TransformationProcessor} and the
	 *                                  {@code inputProcessingConfiguration}s belong
	 *                                  to multiple {@link KnowledgeBase}
	 * @throws NoSuchElementException   if {@code processor} is a
	 *                                  {@link TransformationProcessor} and none of
	 *                                  the {@code inputProcessingConfiguration}s
	 *                                  belongs to exactly {@link KnowledgeBase}
	 */
	public ProcessingConfiguration(ProcessingParameter parameter, Class<? extends Processor> processor,
			Collection<ProcessingConfiguration> inputProcessingConfigurations)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(parameter, processor);

		// SourceProcessor are forbidden: use another constructor
		if (this.isSourceProcessingConfiguration()) {
			throw new IllegalArgumentException(
					"Illegal processor class type \"" + SourceProcessor.class.getCanonicalName() + "\".");
		}

		// add associations between ProcessingConfigurations (both directions)
		this.inputProcessingConfigurations.addAll(inputProcessingConfigurations);
		for (ProcessingConfiguration inputProcessingConfiguration : inputProcessingConfigurations) {
			inputProcessingConfiguration.subsequentProcessingConfigurations.add(this);
		}

		// TODO improve using #hashCode and # equals
		for (ProcessingConfiguration configuration : this.inputProcessingConfigurations) {
			if (configuration.knowledgeBase == null) {
				// configuration belongs to multiple knowledge bases
				if (this.isTransformationProcessingConfiguration()) {
					throw new IllegalStateException(
							"Configuration for transformation processore is assigned to multiple knowledge bases.");
				} else {
					this.knowledgeBase = null; // this belongs to multiple knowledge bases
					break;
				}
			} else {
				// configuration belongs to one knowledge bases
				if (this.knowledgeBase != null) {
					// earlier configuration has been visited
					if (!configuration.knowledgeBase.equals(configuration.knowledgeBase)) {
						// earlier configuration belongs to another knowledge bases

						if (this.isTransformationProcessingConfiguration()) {
							throw new IllegalStateException(
									"Configuration for transformation processore is assigned to multiple knowledge bases.");
						} else {
							this.knowledgeBase = null; // this belongs to multiple knowledge bases
							break;
						}
					}
				} else {
					// no earlier configuration has been visited
					this.knowledgeBase = configuration.knowledgeBase; // this belongs to the knowledge bases of
																		// configuration
				}
			}
		}
		if (this.knowledgeBase == null && this.isTransformationProcessingConfiguration()) {
			// this is a TransformationProcessing Configuration and 
			throw new NoSuchElementException(
					"Configuration for transformation processore is not assigned to a knowledge base.");
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
	public ProcessingConfiguration(ProcessingParameter parameter, Class<? extends SourceProcessor> processor,
			KnowledgeBaseModule knowledgeBaseModule) {
		this(parameter, processor);
		this.knowledgeBaseModule = knowledgeBaseModule;
		this.knowledgeBase = knowledgeBaseModule.knowledgeBase;
	}

	public Collection<ProcessingConfiguration> getInputProcessingConfigurations() {
		return this.inputProcessingConfigurations;
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
		return this.processor.getDeclaredConstructor().newInstance();
	}

	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public Collection<ProcessingConfiguration> getSubsequentProcessingConfigurations() {
		return this.subsequentProcessingConfigurations;
	}

	/**
	 * @return {@code true}, if this is the configuration of a
	 *         {@link MetaProcessor}.
	 */
	public boolean isMetaProcessingConfiguration() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true}, if this is the configuration of a
	 *         {@link SourceProcessor}.
	 */
	public boolean isSourceProcessingConfiguration() {
		return SourceProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true}, if this is the configuration of a
	 *         {@link TransformationProcessor}.
	 */
	public boolean isTransformationProcessingConfiguration() {
		return TransformationProcessor.class.isAssignableFrom(this.processor);
	}

	@Override
	public String toString() {
		return String.format("ProcessingConfiguration[id=%s]", this.id);
	}
}
