package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collection;
import java.util.Collections;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Dyadic;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.compose.Polyadic;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;

public abstract class AbstractTransformationProcessor extends AbstractRefinementProcessor
		implements TransformationProcessor {

	/**
	 * The {@link MultiUnion} of previous data result {@link Graph}s.
	 */
	protected final MultiUnion inputGraph = new MultiUnion();

	@Override
	public void addInputGraph(Graph inputGraph) {
		// merge input graphs to one graph
		if (inputGraph instanceof Polyadic) {
			((Polyadic) inputGraph).getSubGraphs().forEach(this.inputGraph::addGraph);
		} else if (inputGraph instanceof Dyadic) {
			this.inputGraph.addGraph((Graph) ((Dyadic) inputGraph).getL());
			this.inputGraph.addGraph((Graph) ((Dyadic) inputGraph).getL());
		} else {
			this.inputGraph.addGraph(inputGraph);
		}
	}

	@Override
	public Collection<Graph> getDataGraphs() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		MultiUnion result = new MultiUnion(this.inputGraph.getSubGraphs().iterator());
		result.addGraph(this.getResultGraph());
		return Collections.singleton(result);
	}

	@Override
	public Graph getMetaGraph() {
		return this.metaGraph;
	}

}
