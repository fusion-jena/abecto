package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collections;
import java.util.Map;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractTransformationProcessor {

	@Override
	public RdfGraph computeResultGraph() {
		// reasoning
		Model model = ModelFactory.createModelForGraph(this.sourceGraph);
		// TODO suppress progress logging
		// TODO listen progress
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), model);

		// execute and process result
		Model inferences = inferenceModel.difference(model);
		return new RdfGraph(inferences);
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.emptyMap();
	}

}
