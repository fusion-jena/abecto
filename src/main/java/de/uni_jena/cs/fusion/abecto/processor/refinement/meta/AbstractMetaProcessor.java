package de.uni_jena.cs.fusion.abecto.processor.refinement.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;

public abstract class AbstractMetaProcessor extends AbstractRefinementProcessor implements MetaProcessor {

	/**
	 * The {@link MultiUnion}s of previous data result {@link Graph}s.
	 */
	protected final List<Graph> inputGraphs = new ArrayList<>();

	@Override
	public void addInputGraph(Graph inputGraph) {
		this.inputGraphs.add(inputGraph);
	}

	@Override
	public Collection<Graph> getDataGraphs() {
		return this.inputGraphs;
	}

	@Override
	public MultiUnion getMetaGraph() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		MultiUnion result = new MultiUnion(this.metaGraph.getSubGraphs().iterator());
		result.addGraph(this.getResultGraph());
		return result;
	}
}
