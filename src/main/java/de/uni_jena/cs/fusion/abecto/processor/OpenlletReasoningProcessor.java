package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractTransformationProcessor;
import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractTransformationProcessor<WithoutParameters> {
	
	@Override
	public Model computeResultModel() {
		// prepare reasoning
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), this.inputModelUnion);

		// execute and process result
		// TODO suppress progress logging
		Model inferences = inferenceModel.difference(this.inputModelUnion);

		return inferences;
	}

}
