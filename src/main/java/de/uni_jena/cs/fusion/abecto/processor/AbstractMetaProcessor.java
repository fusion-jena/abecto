package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractMetaProcessor extends AbstractProcessor implements MetaProcessor {

	/**
	 * The {@link Graph} groups to process.
	 */
	protected List<Graph> sourceGraphs = new ArrayList<>();
	/**
	 * The {@link Graph} of known mappings and anti-mappings.
	 */
	protected Graph metaGraph;

	@Override
	public void addSourcesGroup(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.sourceGraphs.add(graphUnion);

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourceGraph = source.getGraph();
			graphUnion.addGraph(sourceGraph);
		}
	}

	@Override
	public void setMetaGroup(Collection<RdfGraph> metaGraphs) {
		// create new graph union
		MultiUnion metaGraphUnion = new MultiUnion();
		this.metaGraph = metaGraphUnion;

		// add read only source graphs
		for (RdfGraph metaGraph : metaGraphs) {
			Graph mappingGraph = metaGraph.getGraph();
			metaGraphUnion.addGraph(mappingGraph);
		}
	}
}
