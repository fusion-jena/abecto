package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractSourceProcessor<P extends ParameterModel> extends AbstractProcessor<P>
		implements SourceProcessor<P> {

	protected UUID ontology;

	@Override
	public void setOntology(UUID uuid) {
		this.ontology = uuid;
	}

	@Override
	public Map<UUID, Collection<Model>> getDataModels() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result model is not avaliable.");
		}
		if (this.ontology == null) {
			throw new IllegalStateException("UUID of ontology not set.");
		}
		return Collections.singletonMap(this.ontology, Collections.singleton(this.getResultModel()));
	}

	@Override
	public Collection<Model> getMetaModels() {
		return Collections.emptySet();
	}

	@Override
	public UUID getOntology() {
		return ontology;
	}

	@Override
	protected void prepare() throws InterruptedException {
		// do nothing
	}
}
