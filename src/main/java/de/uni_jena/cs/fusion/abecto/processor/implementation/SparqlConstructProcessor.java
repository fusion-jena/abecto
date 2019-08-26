package de.uni_jena.cs.fusion.abecto.processor.implementation;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractTransformationProcessor;

public class SparqlConstructProcessor
		extends AbstractTransformationProcessor<SparqlConstructProcessor.Parameter> {

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

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public String query;
	}
}
