package de.uni_jena.cs.fusion.abecto.step;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.processor.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.MetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.util.EntityToIdConverter;

@Entity
public class Step extends AbstractEntityWithUUID {

	/**
	 * The {@link Project} this {@link Step} belongs to.
	 */
	@ManyToOne(optional = false)
	@JsonSerialize(converter = EntityToIdConverter.class)
	protected Project project;
	/**
	 * The {@link Ontology} this {@link Step} of a {@link SourceProcessor} belongs
	 * to or {@code null}, if this does not belong to a {@link SourceProcessor}.
	 */
	@ManyToOne
	@JsonSerialize(converter = EntityToIdConverter.class)
	protected Ontology ontology;

	@SuppressWarnings("rawtypes")
	protected Class<? extends Processor> processor;
	@ManyToOne
	protected Parameter parameter;
	@ManyToMany
	protected Collection<Step> inputStep = new HashSet<>();
	@ManyToMany(mappedBy = "inputStep")
	protected Collection<Step> outputSteps = new HashSet<>();

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
	 * @param parameter The {@link Parameter} to use.
	 * @param processor The {@link SourceProcessor} to use.
	 * @param ontology  The {@link Ontology} to assign the {@link Step} to.
	 */
	public Step(Class<Processor<?>> processor, Parameter parameter, Ontology ontology) {
		this(processor, parameter);
		this.ontology = ontology;
		this.project = ontology.getProject();
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

	public Ontology getOntology() {
		return this.ontology;
	}

	public Parameter getParameter() {
		return this.parameter;
	}

	@SuppressWarnings("rawtypes")
	public Class<? extends Processor> getProcessorClass() {
		return this.processor;
	}

	public Project getProject() {
		return this.project;
	}

	/**
	 * @return {@code true} if the processor is a {@link MappingProcessor},
	 *         otherwise {@code false}
	 */
	@JsonIgnore
	public boolean isMapping() {
		return MappingProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link MetaProcessor}, otherwise
	 *         {@code false}
	 */
	@JsonIgnore
	public boolean isMeta() {
		return MetaProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link RefinementProcessor},
	 *         otherwise {@code false}
	 */
	@JsonIgnore
	public boolean isRefinement() {
		return RefinementProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link SourceProcessor}, otherwise
	 *         {@code false}
	 */
	@JsonIgnore
	public boolean isSource() {
		return SourceProcessor.class.isAssignableFrom(this.processor);
	}

	/**
	 * @return {@code true} if the processor is a {@link TransformationProcessor},
	 *         otherwise {@code false}
	 */
	@JsonIgnore
	public boolean isTransformation() {
		return TransformationProcessor.class.isAssignableFrom(this.processor);
	}

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

}
