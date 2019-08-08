package de.uni_jena.cs.fusion.abecto.processor.api;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

public interface RefinementProcessor<P extends ParameterModel> extends Processor<P> {

	/**
	 * Add a {@link Model} to process.
	 * 
	 * @param inputModel {@link Model}s to process
	 */
	public void addInputModelGroup(UUID uuid, Collection<Model> inputModelGroup);

	/**
	 * Add {@link Model}s to process.
	 * 
	 * @param inputModelGroups {@link Model}s to process
	 */
	default public void addInputModelGroups(Map<UUID, Collection<Model>> inputModelGroups) {
		for (Entry<UUID, Collection<Model>> inputModelGroup : inputModelGroups.entrySet()) {
			this.addInputModelGroup(inputModelGroup.getKey(), inputModelGroup.getValue());
		}
	}

	/**
	 * Add a {@link Processor} this {@link Processor} depends on.
	 * 
	 * @param processor {@link Processor} this {@link Processor} depends on
	 */
	public void addInputProcessor(Processor<?> processor);

	/**
	 * Add the previous meta {@link Model}.
	 * 
	 * @param metaModel previous meta {@link Model}
	 */
	public void addMetaModels(Collection<Model> metaModels);
}
