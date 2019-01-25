package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import openllet.jena.PelletReasoner;

public class OpenlletReasoningProcessor extends AbstractProcessor implements SubsequentProcessor {

	private Graph graph;

	@Override
	public RdfGraph call() {
		try {
			// reasoning
			Model model = ModelFactory.createModelForGraph(this.graph);
			// TODO suppress progress logging
			// TODO listen progress
			InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), model);

			// execute and process result
			Model inferences = inferenceModel.difference(model);
			return new RdfGraph(inferences);
		} catch (QueryException e) {
			this.listener.onFailure(e);
			throw e;
		}
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.emptyMap();
	}

	@Override
	public void setSources(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.graph = graphUnion;

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourcegraph = source.getGraph();
			graphUnion.addGraph(sourcegraph);
		}
	}

}
