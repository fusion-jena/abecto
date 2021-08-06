package de.uni_jena.cs.fusion.abecto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
	private final Collection<Resource> outputModelIris = new ArrayList<>();

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

		// set processor parameter
		NodeIterator parameters = configurationModel.listObjectsOfProperty(stepIri, AV.hasParameter);
		while (parameters.hasNext()) {
			Resource parameter = parameters.next().asResource();
			String key = parameter.getProperty(AV.key).getString();
			Object value = parameter.getProperty(RDF.value).getObject();
			processor.setParameter(key, value);
		}

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
			inputModelIris.addAll(inputStep.outputModelIris);
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
			outputModelIris.add(outputModelIri);
			outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
			outputModelIri.addProperty(DQV.computedOn, datasetIri);
			Model outputModel = ModelFactory.createDefaultModel();
			dataset.addNamedModel(outputModelIri.getURI(), outputModel);
		}
		{// prepare general output meta model
			Resource outputModelIri = configurationModel.createResource(AV.MetaDataGraph);
			outputModelIris.add(outputModelIri);
			outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
			Model outputModel = ModelFactory.createDefaultModel();
			dataset.addNamedModel(outputModelIri.getURI(), outputModel);
		}
		// prepare output primary model, if needed
		Models.assertOneOptional(configurationModel.listObjectsOfProperty(stepIri, AV.associatedDataset))
				.ifPresent(datasetIri -> {
					Resource outputModelIri = configurationModel.createResource(AV.PrimaryDataGraph);
					outputModelIris.add(outputModelIri);
					outputModelIri.addProperty(PROV.wasGeneratedBy, stepExecutionIri);
					outputModelIri.addProperty(AV.associatedDataset, datasetIri);
					Model outputModel = ModelFactory.createDefaultModel();
					dataset.addNamedModel(outputModelIri.getURI(), outputModel);
				});

		// run the processor
		stepExecutionIri.addLiteral(PROV.startedAtTime, LocalDateTime.now());
		processor.run();
		stepExecutionIri.addLiteral(PROV.endedAtTime, LocalDateTime.now());

		// remove unused output data models
		for (Resource outputModelIri : outputModelIris) {
			if (dataset.getNamedModel(outputModelIri.getURI()).isEmpty()) {
				dataset.removeNamedModel(outputModelIri.getURI());
				outputModelIri.removeProperties();
			}
		}
		processor.removeEmptyModels();
	}

	public Resource getStepExecution() {
		return stepExecutionIri;
	}

}
