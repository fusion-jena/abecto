/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.Lock;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.processor.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;
import de.uni_jena.cs.fusion.abecto.vocabulary.PROV;

public class Step implements Runnable {

	private final Dataset dataset;
	private final Resource stepIri;
	private Resource stepExecutionIri;
	private final Processor<?> processor;
	private final Model configurationModel;
	private final Collection<Step> inputSteps;
	private final Collection<Resource> inputModelIris = new ArrayList<>();
	private final Map<Resource, Model> outputModelByIri = new HashMap<>();
	private final Logger logger;

	/**
	 * Creates an {@link Step} as defined in the configuration model of an ABECTO
	 * execution plan and the associated {@link Processor} instance, sets the
	 * {@link Processor Processors} parameters and provides the {@link Aspect}
	 * instances.
	 * 
	 * @param dataset            the {@link Dataset} that contains the ABECTO
	 *                           execution plan and will contain result models of
	 *                           the {@link Step Steps}
	 * @param configurationModel the configuration model describing the ABECTO
	 *                           execution plan, which is part of the
	 *                           {@code dataset}.
	 * @param stepIri            the IRI of this step in the {@code dataset}
	 * @param inputSteps         the objects of the input {@link Step} of this
	 *                           {@link Step}
	 * @param aspects            the {@link Aspect Aspect} instances for the ABECTO
	 *                           execution plan.
	 * 
	 * 
	 * @throws ClassCastException           if this {@link Step Steps} processor
	 *                                      class was not found
	 * @throws IllegalArgumentException     if the IRI of this {@link Step Steps}
	 *                                      processor class is ill-formed
	 * @throws ReflectiveOperationException if this {@link Step Steps} processor
	 *                                      class could not be instantiated
	 */
	@SuppressWarnings("unchecked")
	public Step(File relativeBasePath, Dataset dataset, Model configurationModel, Resource stepIri,
			Collection<Step> inputSteps, Aspect... aspects)
			throws IllegalArgumentException, ClassCastException, ReflectiveOperationException {
		this.dataset = dataset;
		this.configurationModel = configurationModel;
		this.stepIri = stepIri;
		this.inputSteps = inputSteps;

		// get processor instance
		// Note: done in constructor to fail early
		// Note: expecting unofficial java scheme resource, e.g. "java:java.util.List"
		String classUri = Models.assertOne(configurationModel.listObjectsOfProperty(stepIri, AV.processorClass))
				.asResource().getURI();
		if (!classUri.startsWith("java:")) {
			throw new IllegalArgumentException(
					"Failed to load processor class. Expected IRI of scheme \"java:\", but got: " + classUri);
		}
		Class<?> processorClass = Class.forName(classUri.substring(5));
		if (!Processor.class.isAssignableFrom(processorClass)) {
			throw new IllegalArgumentException("Failed to load processor class. Expected subclass of \""
					+ Processor.class.getCanonicalName() + "\", but got: " + classUri);
		}
		processor = ((Class<Processor<?>>) processorClass).getDeclaredConstructor(new Class[0]).newInstance();

		// get processor parameter
		Map<String, List<?>> parameters = new HashMap<>();
		NodeIterator parameterIterator = configurationModel.listObjectsOfProperty(stepIri, AV.hasParameter);
		while (parameterIterator.hasNext()) {
			Resource parameter = parameterIterator.next().asResource();
			String key = parameter.getRequiredProperty(AV.key).getString();
			List<Object> values = new ArrayList<>();
			parameter.listProperties(AV.value).mapWith(Statement::getObject)
					.mapWith(o -> o.isLiteral() ? o.asLiteral().getValue() : o.asResource())
					.forEach(values::add);
			parameters.put(key, values);
		}
		Parameters.setParameters(processor, parameters);

		// set aspects
		processor.addAspects(aspects);

		// set relative base path
		processor.setRelativeBasePath(relativeBasePath);

		// set logger
		this.logger = LoggerFactory.getLogger(processorClass);
	}

	/**
	 * Executes the processor to produce the result models.
	 * <p>
	 * <strong>Note:</strong> This method might run concurrently. Therefore,
	 * <a href=
	 * "http://jena.apache.org/documentation/notes/concurrency-howto.html">locks on
	 * the configuration model</a> are used to avoid dangerous concurrent
	 * updates.
	 */
	@Override
	public void run() {
		configurationModel.enterCriticalSection(Lock.WRITE);
		try {
			// write provenance data to configuration model
			stepExecutionIri = configurationModel.createResource(AV.StepExecution);
			stepExecutionIri.addProperty(PPlan.correspondsToStep, this.stepIri);

			// add input models
			for (Step inputStep : inputSteps) {
				processor.addInputProcessor(inputStep.processor);
				inputModelIris.addAll(inputStep.inputModelIris);
				inputModelIris.addAll(inputStep.outputModelByIri.keySet());
			}
			configurationModel.listObjectsOfProperty(stepIri, AV.predefinedMetaDataGraph).mapWith(RDFNode::asResource)
					.forEach(inputMetaModelIri -> {
						Model inputMetaModel = MappingProcessor
								.inferTransitiveCorrespondences(dataset.getNamedModel(inputMetaModelIri));
						ExtendedIterator<Resource> computedOnDatasetIterator = configurationModel
								.listObjectsOfProperty(inputMetaModelIri, DQV.computedOn)
								.filterKeep(RDFNode::isResource).mapWith(RDFNode::asResource);
						if (computedOnDatasetIterator.hasNext()) {
							computedOnDatasetIterator
									.forEach(computedOn -> processor.addInputMetaModel(computedOn, inputMetaModel));
						} else {
							processor.addInputMetaModel(null, inputMetaModel);
						}
						inputModelIris.add(inputMetaModelIri);
					});
			for (Resource inputModelIri : inputModelIris) {
				stepExecutionIri.addProperty(PROV.used, inputModelIri);
			}

			// set associated dataset and prepare associated output metamodel, if needed
			Models.assertOneOptional(configurationModel.listObjectsOfProperty(stepIri, AV.associatedDataset))
					.map(RDFNode::asResource).ifPresent(processor::setAssociatedDataset);

			// run the processor
			stepExecutionIri.addLiteral(PROV.startedAtTime, new GregorianCalendar());
		} finally {
			configurationModel.leaveCriticalSection();
		}

		Optional<String> stepLabel = configurationModel.listObjectsOfProperty(stepIri, RDFS.label).nextOptional()
				.map(RDFNode::asLiteral).map(Literal::getString);
		logger.info(String.format("Execution of Step \"%s\" started.", stepLabel.orElse(stepIri.toString())));
		processor.run();
		logger.info(String.format("Execution of Step \"%s\" completed.", stepLabel.orElse(stepIri.toString())));

		configurationModel.enterCriticalSection(Lock.WRITE);
		try {
			stepExecutionIri.addLiteral(PROV.endedAtTime, new GregorianCalendar());
			// set output primary model metadata, if applicable
			processor.getAssociatedDataset().ifPresent(datasetIri -> {
				Optional<Model> outputModel = processor.getOutputPrimaryModel();
				if (outputModel.isPresent() && !outputModel.get().isEmpty()) {
					Resource outputModelIri = configurationModel.createResource(AV.PrimaryDataGraph);
					outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
					outputModelIri.addProperty(AV.associatedDataset, datasetIri);
					dataset.addNamedModel(outputModelIri, outputModel.get());
				}
			});
			// set metadata for output meta model of input dataset
			for (Resource datasetIri : processor.getDatasets()) {
				Model outputModel = processor.getOutputMetaModel(datasetIri);
				if (!outputModel.isEmpty()) {
					Resource outputModelIri = configurationModel.createResource(AV.MetaDataGraph);
					outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
					outputModelIri.addProperty(DQV.computedOn, datasetIri);
					dataset.addNamedModel(outputModelIri, outputModel);
				}
			}
			// set metadata for general metamodel
			Model outputModel = processor.getOutputMetaModel(null);
			if (!outputModel.isEmpty()) {
				Resource outputModelIri = configurationModel.createResource(AV.MetaDataGraph);
				outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
				dataset.addNamedModel(outputModelIri, outputModel);
			}

			// remove empty output models from processor
			processor.removeEmptyModels();
		} finally {
			configurationModel.leaveCriticalSection();
		}
	}

	public Resource getStepExecution() {
		return stepExecutionIri;
	}

}
