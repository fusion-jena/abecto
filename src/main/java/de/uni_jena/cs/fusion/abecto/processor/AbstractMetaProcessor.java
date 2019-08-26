package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractMetaProcessor<P extends ParameterModel> extends AbstractRefinementProcessor<P> implements MetaProcessor<P> {

	@Override
	public Map<UUID, Collection<Model>> getDataModels() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<Model> getMetaModel() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Model is not avaliable.");
		}
		Collection<Model> result = new HashSet<>(this.metaSubModels);
		result.add(this.getResultModel());
		return result;
	}
}
