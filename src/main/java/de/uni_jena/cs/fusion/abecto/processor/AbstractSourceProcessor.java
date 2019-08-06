package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

public abstract class AbstractSourceProcessor<P> extends AbstractProcessor<P> implements SourceProcessor<P> {

	protected UUID knowledgBase;

	@Override
	public void setKnowledgBase(UUID uuid) {
		this.knowledgBase = uuid;
	}

	@Override
	public Map<UUID, Collection<Model>> getDataModels() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result model is not avaliable.");
		}
		if (this.knowledgBase == null) {
			throw new IllegalStateException("UUID of knowledg base not set.");
		}
		return Collections.singletonMap(this.knowledgBase, Collections.singleton(this.getResultModel()));
	}

	@Override
	public Collection<Model> getMetaModel() {
		return Collections.emptySet();
	}

	@Override
	protected void prepare() throws InterruptedException {
		// do nothing
	}
}
