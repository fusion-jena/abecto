package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.Collection;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractRefinementProcessor extends AbstractProcessor implements RefinementProcessor {
	/**
	 * The hidden {@link MultiUnion} of the {@link #metaGraph}.
	 */
	private final MultiUnion metaGraphUnion = new MultiUnion();
	/**
	 * The {@link Graph} containing previous meta results.
	 */
	protected final Graph metaGraph = metaGraphUnion;

	@Override
	public void addMetaGraphs(Collection<RdfGraph> metaGraphs) {
		// add read only source graphs
		for (RdfGraph source : metaGraphs) {
			Graph sourcegraph = source.getGraph();
			this.metaGraphUnion.addGraph(sourcegraph);
		}
	}
}
