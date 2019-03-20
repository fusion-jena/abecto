package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Dyadic;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.compose.Polyadic;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;

public abstract class AbstractRefinementProcessor extends AbstractProcessor implements RefinementProcessor {

	/**
	 * {@link Processor}s this {@link Processor} depends on.
	 */
	protected final Collection<Processor> dependetProcessors = new ArrayList<>();

	/**
	 * The {@link MultiUnion} of previous meta result {@link Graph}s.
	 */
	protected final MultiUnion metaGraph = new MultiUnion();

	@Override
	public void addDependetProcessor(Processor dependedProcessor) {
		this.dependetProcessors.add(dependedProcessor);
	}

	@Override
	public void addMetaGraph(Graph metaGraph) {
		// merge input graphs to one graph
		if (metaGraph instanceof Polyadic) {
			((Polyadic) metaGraph).getSubGraphs().forEach(this.metaGraph::addGraph);
		} else if (metaGraph instanceof Dyadic) {
			this.metaGraph.addGraph((Graph) ((Dyadic) metaGraph).getL());
			this.metaGraph.addGraph((Graph) ((Dyadic) metaGraph).getL());
		} else {
			this.metaGraph.addGraph(metaGraph);
		}
	}

	@Override
	public void prepare() throws InterruptedException {
		for (Processor dependedProcessor : this.dependetProcessors) {
			while (!dependedProcessor.isSucceeded()) {
				if (dependedProcessor.isFailed()) {
					this.fail();
				}
				dependedProcessor.await();
			}
			this.addMetaGraph(dependedProcessor.getMetaGraph());
			this.addInputGraph(dependedProcessor.getDataGraphs());
		}
	}
}
