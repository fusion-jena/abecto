package de.uni_jena.cs.fusion.abecto.processor.transformation;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
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
		// TODO listen progress
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), model);

		// execute and process result
		// TODO suppress progress logging
		Model inferences = inferenceModel.difference(model);
		return new RdfGraph(inferences);
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.emptyMap();
	}

}
