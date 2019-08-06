package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractTransformationProcessor;

public class SparqlConstructProcessor extends AbstractTransformationProcessor<SparqlConstructProcessorParameter> {

	@Override
	public Model computeResultModel() {
		// prepare query
		Query query = QueryFactory.create(this.getParameters().query);

		// prepare execution
		QueryExecution queryExecution = QueryExecutionFactory.create(query, this.inputModelUnion);

		// execute and process result
		Model constructedModel = queryExecution.execConstruct();
		return constructedModel;
	}

	@Override
	public Class<SparqlConstructProcessorParameter> getParameterModel() {
		return SparqlConstructProcessorParameter.class;
	}

}
