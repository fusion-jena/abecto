/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.Lock;
import org.apache.jena.vocabulary.RDF;

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
	 * @param aspectMap          the {@link Aspect Aspect} instances for the ABECTO
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
	public Step(Dataset dataset, Model configurationModel, Resource stepIri, Collection<Step> inputSteps,
			Map<Resource, Aspect> aspectMap)
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
			parameter.listProperties(RDF.value).forEach(stmt -> values.add(stmt.getObject().asLiteral().getValue()));
			parameters.put(key, values);
		}
		Parameters.setParameters(processor, parameters);

		// set aspects
		processor.setAspectMap(aspectMap);
	}

	private void prepareOutputMetaModel(Resource datasetIri) {
		Model outputModel = ModelFactory.createDefaultModel();
		setOutputModelMetadata(AV.MetaDataGraph, datasetIri, outputModel);
		processor.setOutputMetaModel(datasetIri, outputModel);
	}

	private void setOutputModelMetadata(Resource modelType, Resource datasetIri, Model outputModel) {
		// TODO remove workaround for https://issues.apache.org/jira/browse/JENA-2169
		Resource outputModelIri = configurationModel.createResource("uuid:" + UUID.randomUUID(), modelType);
		outputModelByIri.put(outputModelIri, outputModel);
		outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
		if (datasetIri != null) {
			outputModelIri.addProperty(modelType.equals(AV.MetaDataGraph) ? DQV.computedOn : AV.associatedDataset,
					datasetIri);
		}
	}

	/**
	 * Executes the processor to produce the result models.
	 * <p>
	 * <strong>Note:</strong> This method might run concurrently. Therefore,
	 * <a href=
	 * "http://jena.apache.org/documentation/notes/concurrency-howto.html">locks on
	 * the configuration model</a> are used to to avoid dangerous concurrent
	 * updates.
	 */
	@Override
	public void run() {
		configurationModel.enterCriticalSection(Lock.WRITE);
		try {
			// write provenance data to configuration model
			stepExecutionIri = configurationModel.createResource(AV.StepExecution);
			stepExecutionIri.addProperty(PPlan.correspondsToStep, this.stepIri);

			// get input models
			for (Step inputStep : inputSteps) {
				processor.addInputProcessor(inputStep.processor);
				inputModelIris.addAll(inputStep.inputModelIris);
				inputModelIris.addAll(inputStep.outputModelByIri.keySet());
			}
			configurationModel.listObjectsOfProperty(stepIri, AV.inputMetaDataGraph).forEach(object -> {
				Resource inputMetaModelIri = object.asResource();
				processor.addInputMetaModel(null, dataset.getNamedModel(inputMetaModelIri.getURI()));
				inputModelIris.add(inputMetaModelIri);
			});
			for (Resource inputModelIri : inputModelIris) {
				stepExecutionIri.addProperty(PROV.used, inputModelIri);
			}

			// prepare output meta models for each input dataset
			Set<Resource> inputDatasets = processor.getInputDatasets();
			for (Resource datasetIri : processor.getInputDatasets()) {
				prepareOutputMetaModel(datasetIri);
			}
			// prepare general output meta model
			prepareOutputMetaModel(null);
			// set associated dataset and prepare associated output meta model, if needed
			Models.assertOneOptional(configurationModel.listObjectsOfProperty(stepIri, AV.associatedDataset))
					.ifPresent(datasetNode -> {
						Resource datasetIri = datasetNode.asResource();
						processor.setAssociatedDataset(datasetIri);
						if (!inputDatasets.contains(datasetIri)) {
							prepareOutputMetaModel(datasetIri);
						}
					});

			// run the processor
			stepExecutionIri.addLiteral(PROV.startedAtTime, OffsetDateTime.now());

		} finally {
			configurationModel.leaveCriticalSection();
		}

		processor.run();

		configurationModel.enterCriticalSection(Lock.WRITE);
		try {
			stepExecutionIri.addLiteral(PROV.endedAtTime, OffsetDateTime.now());
			Models.assertOneOptional(configurationModel.listObjectsOfProperty(stepIri, AV.associatedDataset))
					.ifPresent(datasetNode -> {
						setOutputModelMetadata(AV.PrimaryDataGraph, datasetNode.asResource(),
								processor.getOutputPrimaryModel().get());
					});

			// remove metadata of empty output models, add others to dataset
			for (Resource outputModelIri : outputModelByIri.keySet()) {
				Model outputModel = outputModelByIri.get(outputModelIri);
				if (outputModel.isEmpty()) {
					outputModelIri.removeProperties();
				} else {
					dataset.addNamedModel(outputModelIri.getURI(), outputModel);
				}
			}
			processor.removeEmptyModels();
		} finally {
			configurationModel.leaveCriticalSection();
		}
	}

	public Resource getStepExecution() {
		return stepExecutionIri;
	}

}
