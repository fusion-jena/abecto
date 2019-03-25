package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

public class SparqlConstructProcessor extends AbstractTransformationProcessor {

	@Override
	public Model computeResultModel() {
		// prepare query
		String queryString = this.getProperty("query", new TypeLiteral<String>() {});
		Query query = QueryFactory.create(queryString);

		// prepare execution
		QueryExecution queryExecution = QueryExecutionFactory.create(query, this.inputModelUnion);

		// execute and process result
		Model constructedModel = queryExecution.execConstruct();
		return constructedModel;
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("query", new TypeLiteral<String>() {});
	}

}
