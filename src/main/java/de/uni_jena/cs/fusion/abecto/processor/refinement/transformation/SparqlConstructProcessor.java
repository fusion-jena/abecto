package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class SparqlConstructProcessor extends AbstractTransformationProcessor {

	@Override
	public Graph computeResultGraph() {
		// prepare query
		String queryString = this.getProperty("query", new TypeLiteral<String>() {
		});
		Query query = QueryFactory.create(queryString);

		// prepare execution
		Model model = ModelFactory.createModelForGraph(this.inputGraph);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);

		// execute and process result
		Model constructedModel = queryExecution.execConstruct();
		return constructedModel.getGraph();
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("query", new TypeLiteral<String>() {
		});
	}

}
