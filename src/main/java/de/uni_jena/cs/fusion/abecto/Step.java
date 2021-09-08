package de.uni_jena.cs.fusion.abecto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
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
	private final Processor processor;
	private final Model configurationModel;
	private final Collection<Step> inputSteps;
	private final Collection<Resource> inputModelIris = new ArrayList<>();
	private final Map<Resource, Model> outputModelByIri = new HashMap<>();

	public Step(Dataset dataset, Model configurationModel, Resource stepIri, Collection<Step> inputSteps,
			Map<Resource, Aspect> aspects) throws ClassCastException, ReflectiveOperationException {
		this.dataset = dataset;
		this.configurationModel = configurationModel;
		this.stepIri = stepIri;
		this.inputSteps = inputSteps;

		// get processor instance
		// Note: done in constructor to fail early
		@SuppressWarnings("unchecked")
		Class<Processor> processorClass = (Class<Processor>) Models
				.assertOne(configurationModel.listObjectsOfProperty(stepIri, AV.processorClass)).asLiteral().getValue();
		processor = processorClass.getDeclaredConstructor(new Class[0]).newInstance();

		// get processor parameter
		Map<String, List<?>> parameters = new HashMap<>();
		NodeIterator parameterIterator = configurationModel.listObjectsOfProperty(stepIri, AV.hasParameter);
		while (parameterIterator.hasNext()) {
			Resource parameter = parameterIterator.next().asResource();
			String key = parameter.getRequiredProperty(AV.key).getString();
			List<Object> values = new ArrayList<>();
			parameter.listProperties(RDF.value).forEach(values::add);
			parameters.put(key, values);
		}
		Parameters.setParameters(processor, parameters);

		// set aspects
		processor.setAspects(aspects);
	}

	@Override
	public void run() {
		// TODO concurrency relevant for updates on configurationModel?

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
			processor.addInputMetaModel(inputMetaModelIri, dataset.getNamedModel(inputMetaModelIri.getURI()));
			inputModelIris.add(inputMetaModelIri);
		});
		for (Resource inputModelIri : inputModelIris) {
			stepExecutionIri.addProperty(PROV.used, inputModelIri);
		}

		// prepare output models
		for (Resource datasetIri : processor.getInputDatasets()) {
			// prepare output meta model for a dataset
			Resource outputModelIri = configurationModel.createResource(AV.MetaDataGraph);
			Model outputModel = ModelFactory.createDefaultModel();
			outputModelByIri.put(outputModelIri, outputModel);
			outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
			outputModelIri.addProperty(DQV.computedOn, datasetIri);
			processor.setOutputMetaModel(datasetIri, outputModel);
		}
		{// prepare general output meta model
			Resource outputModelIri = configurationModel.createResource(AV.MetaDataGraph);
			Model outputModel = ModelFactory.createDefaultModel();
			outputModelByIri.put(outputModelIri, outputModel);
			outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
			processor.setOutputMetaModel(null, outputModel);
		}
		// prepare output primary model, if needed
		Models.assertOneOptional(configurationModel.listObjectsOfProperty(stepIri, AV.associatedDataset))
				.ifPresent(datasetIri -> {
					Resource outputModelIri = configurationModel.createResource(AV.PrimaryDataGraph);
					Model outputModel = ModelFactory.createDefaultModel();
					outputModelByIri.put(outputModelIri, outputModel);
					outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
					outputModelIri.addProperty(AV.associatedDataset, datasetIri);
					processor.setOutputPrimaryModel(datasetIri.asResource(), outputModel);
				});

		// run the processor
		stepExecutionIri.addLiteral(PROV.startedAtTime, LocalDateTime.now());
		processor.run();
		stepExecutionIri.addLiteral(PROV.endedAtTime, LocalDateTime.now());

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
	}

	public Resource getStepExecution() {
		return stepExecutionIri;
	}

}
