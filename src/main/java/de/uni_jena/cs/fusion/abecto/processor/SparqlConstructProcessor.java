package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collections;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class SparqlConstructProcessor extends AbstractTransformationProcessor {

	@Override
	public RdfGraph computeResultGraph() {
		// prepare query
		String queryString = this.getProperty("query", String.class);
		Query query = QueryFactory.create(queryString);

		// prepare execution
		Model model = ModelFactory.createModelForGraph(this.sourceGraph);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);

		// execute and process result
		Model constructedModel = queryExecution.execConstruct();
		return new RdfGraph(constructedModel);
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("query", String.class);
	}

}
