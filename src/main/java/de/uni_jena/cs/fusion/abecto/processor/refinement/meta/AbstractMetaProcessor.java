package de.uni_jena.cs.fusion.abecto.processor.refinement.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractMetaProcessor extends AbstractRefinementProcessor implements MetaProcessor {

	/**
	 * The {@link Graph}s to process.
	 */
	protected List<Graph> inputGraphs = new ArrayList<>();

	@Override
	public void addInputGraphGroup(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.inputGraphs.add(graphUnion);

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourceGraph = source.getGraph();
			graphUnion.addGraph(sourceGraph);
		}
	}
}
