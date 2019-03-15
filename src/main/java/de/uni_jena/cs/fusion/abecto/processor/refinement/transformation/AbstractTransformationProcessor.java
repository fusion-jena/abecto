package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collection;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public abstract class AbstractTransformationProcessor extends AbstractRefinementProcessor
		implements TransformationProcessor {

	/**
	 * The hidden {@link MultiUnion} of the {@link #inputGraph}.
	 */
	private final MultiUnion inputGraphUnion = new MultiUnion();
	/**
	 * The {@link Graph} to process.
	 */
	protected final Graph inputGraph = inputGraphUnion;

	@Override
	public void addInputGraphGroup(Collection<RdfGraph> inputGraphs) {
		// add read only source graphs
		for (RdfGraph source : inputGraphs) {
			Graph sourcegraph = source.getGraph();
			this.inputGraphUnion.addGraph(sourcegraph);
		}
	}
}
