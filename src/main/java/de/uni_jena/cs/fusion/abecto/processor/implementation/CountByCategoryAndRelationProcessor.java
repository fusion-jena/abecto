package de.uni_jena.cs.fusion.abecto.processor.implementation;

import org.apache.jena.rdf.model.Model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;

public class CountByCategoryAndRelationProcessor
		extends AbstractMetaProcessor<CountByCategoryAndRelationProcessor.Parameter> {

	// TODO black- or whitelist

	@Override
	protected Model computeResultModel() throws Exception {
		// TODO get relation paths per KB
		// TODO get category IRIs per KB
		// TODO construct model containing count per relation path, IRI and KB
		return null;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {

	}
}
