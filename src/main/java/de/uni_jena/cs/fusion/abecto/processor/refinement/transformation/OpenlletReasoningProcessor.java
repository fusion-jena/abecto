package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractTransformationProcessor {

	private static final Logger log = LoggerFactory.getLogger(OpenlletReasoningProcessor.class);

	@Override
	public RdfGraph computeResultGraph() {
		log.info("Reasoning started.");
		// reasoning
		Model model = ModelFactory.createModelForGraph(this.inputGraph);
		// TODO listen progress
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), model);

		// execute and process result
		// TODO suppress progress logging
		Model inferences = inferenceModel.difference(model);

		log.info("Reasoning completed.");

		return new RdfGraph(inferences);
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.emptyMap();
	}

}
