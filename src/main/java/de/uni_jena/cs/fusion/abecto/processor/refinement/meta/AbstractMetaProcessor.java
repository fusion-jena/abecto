package de.uni_jena.cs.fusion.abecto.processor.refinement.meta;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;

public abstract class AbstractMetaProcessor extends AbstractRefinementProcessor implements MetaProcessor {

	@Override
	public Map<UUID, Collection<Graph>> getDataGraphs() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<Graph> getMetaGraph() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		Collection<Graph> result = new HashSet<>(this.metaSubGraphs);
		result.add(this.getResultGraph());
		return result;
	}
}
