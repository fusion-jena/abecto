package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

public abstract class AbstractTransformationProcessor extends AbstractRefinementProcessor
		implements TransformationProcessor {

	@Override
	public Map<UUID, Collection<Model>> getDataModels() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Model is not avaliable.");
		}
		Map<UUID, Collection<Model>> result = new HashMap<>();
		for (UUID uuid : this.inputGroupSubModels.keySet()) {
			Collection<Model> collection = result.computeIfAbsent(uuid, (a) -> new HashSet<Model>());
			collection.addAll(this.inputGroupSubModels.get(uuid));
			collection.add(this.getResultModel());
		}
		return result;
	}

	@Override
	public Collection<Model> getMetaModel() {
		return this.metaSubModels;
	}
}
