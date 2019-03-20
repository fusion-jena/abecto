package de.uni_jena.cs.fusion.abecto.processor.source;

import java.util.Collection;
import java.util.Collections;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;

public abstract class AbstractSourceProcessor extends AbstractProcessor implements SourceProcessor {

	@Override
	public Collection<Graph> getDataGraphs() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		return Collections.singleton(this.getResultGraph());
	}

	@Override
	public Graph getMetaGraph() {
		return Graph.emptyGraph;
	}

	@Override
	public void prepare() throws InterruptedException {
		// do nothing
	}
}
