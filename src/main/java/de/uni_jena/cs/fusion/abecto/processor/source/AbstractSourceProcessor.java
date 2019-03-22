package de.uni_jena.cs.fusion.abecto.processor.source;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;

public abstract class AbstractSourceProcessor extends AbstractProcessor implements SourceProcessor {

	protected UUID knowledgBase;

	@Override
	public void setKnowledgBase(UUID uuid) {
		this.knowledgBase = uuid;
	}

	@Override
	public Map<UUID, Collection<Graph>> getDataGraphs() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Graph is not avaliable.");
		}
		if (knowledgBase == null) {
			throw new IllegalStateException("UUID of knowledg base not set.");
		}
		return Collections.singletonMap(knowledgBase, Collections.singleton(this.getResultGraph()));
	}

	@Override
	public Collection<Graph> getMetaGraph() {
		return Collections.emptySet();
	}

	@Override
	protected void prepare() throws InterruptedException {
		// do nothing
	}
}
