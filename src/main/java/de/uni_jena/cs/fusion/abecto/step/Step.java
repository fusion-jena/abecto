package de.uni_jena.cs.fusion.abecto.step;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processor.api.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity
public class Step extends AbstractEntityWithUUID {

	/**
	 * The {@link Project} this {@link Step} belongs to.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	protected Project project;
	/**
	 * The {@link KnowledgeBase} this {@link Step} of a {@link SourceProcessor}
	 * belongs to or {@code null}, if this does not belong to a
	 * {@link SourceProcessor}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	protected KnowledgeBase knowledgeBase;

	@SuppressWarnings("rawtypes")
	protected Class<? extends Processor> processor;
	@ManyToOne
	protected Parameter parameter;
	@ManyToMany(fetch = FetchType.LAZY)
	protected Collection<Step> inputStep = new HashSet<>();
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "inputStep", cascade = CascadeType.REMOVE)
	protected Collection<Step> outputSteps = new HashSet<>();
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "step", cascade = CascadeType.REMOVE)
	protected Collection<Processing> processings = new HashSet<>();

	protected Step() {
	}

	/**
	 * Creates a {@link Step} for a {@link RefinementProcessor}.
	 * 
	 * @param parameter      The {@link Parameter} to use.
	 * @param processorClass The {@link Processor} to use. {@link SourceProcessor}
	 *                       are not allowed.
	 * @param inputSteps     The {@link Step}s whose result to use as input.
	 */
	public Step(Class<Processor<?>> processorClass, Parameter parameter, Iterable<Step> inputSteps)
			throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
		this(processorClass, parameter);

		// add associations between steps
		for (Step inputStep : inputSteps) {
			this.addInputStep(inputStep);
		}

		// set project
		if (this.inputStep.stream().map((step) -> step.project).distinct().count() == 1L) {
			this.project = this.inputStep.iterator().next().project;
		} else {
			throw new IllegalStateException("InputSteps belong to multiple projects.");
		}
	}

	/**
	 * Creates a {@link Step} for a {@link SourceProcessor}.
	 * 
	 * @param parameter           The {@link Parameter} to use.
	 * @param processor           The {@link SourceProcessor} to use.
	 * @param knowledgeBaseModule The {@link KnowledgeBaseModule} to assign the
	 *                            {@link Step} to.
	 */
	public Step(Class<Processor<?>> processor, Parameter parameter, KnowledgeBase knowledgeBase) {
		this(processor, parameter);
		this.knowledgeBase = knowledgeBase;
		this.project = knowledgeBase.getProject();
	}

	/**
	 * Internal {@link Step} constructor for reuse in other constructors only.
	 * 
	 * @param parameter
	 * @param processor
	 */
	protected Step(Class<Processor<?>> processor, Parameter parameter) {
		this.parameter = parameter;
		this.processor = processor;
	}

	@JsonIgnore
	public void addInputStep(Step inputStep) {
		this.inputStep.add(inputStep);
	}

	@JsonIgnore
	public Collection<Step> getInputSteps() {
		return this.inputStep;
	}

	@JsonIgnore
	public KnowledgeBase getKnowledgeBase() {
		return this.knowledgeBase;
	}

	public UUID getKnowledgeBaseId() {
		if (this.knowledgeBase != null) {
			return this.knowledgeBase.getId();
		} else {
			return null;
		}
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
	 * @return {@code true} if the processor is a {@link MappingProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isMapping() {
		return MappingProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link MetaProcessor}, otherwise
	 *         {@code false}
	 */
	public boolean isMeta() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link RefinementProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isRefinement() {
		return RefinementProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link SourceProcessor}, otherwise
	 *         {@code false}
	 */
	public boolean isSource() {
		return SourceProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link TransformationProcessor},
	 *         otherwise {@code false}
	 */
	public boolean isTransformation() {
		return TransformationProcessor.class.isAssignableFrom(this.processor);
	}

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return String.format("Step[id=%s, project=%s]", this.id, this.project.getId());
	}

}
