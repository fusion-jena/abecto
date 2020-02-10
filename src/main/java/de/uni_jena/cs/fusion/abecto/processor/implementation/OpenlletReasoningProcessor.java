package de.uni_jena.cs.fusion.abecto.processor.implementation;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractTransformationProcessor;
import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractTransformationProcessor<EmptyParameters> {

	@Override
	public void computeResultModel() {
		// prepare reasoning
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), this.inputModelUnion);

		// execute and process result
		// TODO suppress progress logging

		this.setModel(inferenceModel.difference(this.inputModelUnion));
	}

}
