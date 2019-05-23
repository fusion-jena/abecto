package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractTransformationProcessor {
	@Override
	public Model computeResultModel() {
		// prepare reasoning
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), this.inputModelUnion);

		// execute and process result
		// TODO suppress progress logging
		Model inferences = inferenceModel.difference(this.inputModelUnion);

		return inferences;
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.emptyMap();
	}

}
