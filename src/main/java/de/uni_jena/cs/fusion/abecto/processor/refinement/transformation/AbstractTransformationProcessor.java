package de.uni_jena.cs.fusion.abecto.processor.refinement.transformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.refinement.AbstractRefinementProcessor;

public abstract class AbstractTransformationProcessor extends AbstractRefinementProcessor
		implements TransformationProcessor {

	@Override
	public Map<UUID, Collection<Graph>> getDataGraphs() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		Map<UUID, Collection<Graph>> result = new HashMap<>();
		for (UUID uuid : inputGroupSubGraphs.keySet()) {
			Collection<Graph> collection = result.computeIfAbsent(uuid, (a) -> new HashSet<Graph>());
			collection.addAll(inputGroupSubGraphs.get(uuid));
			collection.add(this.getResultGraph());
		}
		return result;
	}

	@Override
	public Collection<Graph> getMetaGraph() {
		return this.metaSubGraphs;
	}

}
