package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractMetaProcessor;

public class CountByCategoryAndRelationProcessor extends AbstractMetaProcessor<CountByCategoryAndRelationProcessorParameter> {

	// TODO black- or whitelist

	@Override
	protected Model computeResultModel() throws Exception {
		// TODO get relation paths per KB
		// TODO get category IRIs per KB
		// TODO construct model containing count per relation path, IRI and KB
		return null;
	}

	@Override
	public Class<CountByCategoryAndRelationProcessorParameter> getParameterModel() {
		return CountByCategoryAndRelationProcessorParameter.class;
	}

}
