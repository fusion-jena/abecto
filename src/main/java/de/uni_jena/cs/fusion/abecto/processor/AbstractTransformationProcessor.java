package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractTransformationProcessor extends AbstractProcessor implements TransformationProcessor {

	/**
	 * The {@link Graph} to process.
	 */
	protected Graph sourceGraph;

	@Override
	public void setSources(Collection<RdfGraph> sources) {
		// create new graph union
		MultiUnion graphUnion = new MultiUnion();
		this.sourceGraph = graphUnion;

		// add read only source graphs
		for (RdfGraph source : sources) {
			Graph sourcegraph = source.getGraph();
			graphUnion.addGraph(sourcegraph);
		}
	}

}
