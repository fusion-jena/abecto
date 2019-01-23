package de.uni_jena.cs.fusion.abecto.processor;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.exceptions.ParserException;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class SparqlConstructProcessor extends AbstractProcessor implements SubsequentProcessor {

	private Graph graph;
	private final Graph writableGraph;

	public SparqlConstructProcessor() {
		this.writableGraph = Factory.createGraphMem();
	}

	@Override
	public RdfGraph call() {
		try {
			// prepare query
			String queryString = this.getProperty("query", String.class);
			Query query = QueryFactory.create(queryString);

			// prepare execution
			Model model = ModelFactory.createModelForGraph(this.graph);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			
			// execute and process result
			Model constructedModel = queryExecution.execConstruct();
			return new RdfGraph(constructedModel.getGraph(), "http://example.org/");
		} catch (IOException | ParserException e) {
			this.listener.onFailure(e);
			throw new RuntimeException(e);
		} catch (QueryException e) {
			this.listener.onFailure(e);
			throw e;
		}
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("query", String.class);
	}

	@Override
	public void setSources(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.graph = graphUnion;

		// add the writable graph
		graphUnion.addGraph(this.writableGraph);
		graphUnion.setBaseGraph(this.writableGraph);

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourcegraph = source.getGraph();
			graphUnion.addGraph(sourcegraph);
		}
	}

}
