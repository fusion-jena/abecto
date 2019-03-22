package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.Processor;

public interface RefinementProcessor extends Processor {

	/**
	 * Add the previous meta {@link Graph}.
	 * 
	 * @param metaGraph previous meta {@link Graph}
	 */
	public void addMetaGraphs(Collection<Graph> metaGraphs);

	/**
	 * Add a {@link Graph} to process.
	 * 
	 * @param inputGraph {@link Graph}s to process
	 */
	public void addInputGraphGroup(UUID uuid, Collection<Graph> inputGraphGroup);

	/**
	 * Add {@link Graph}s to process.
	 * 
	 * @param inputGraphGroups {@link Graph}s to process
	 */
	default public void addInputGraphGroups(Map<UUID, Collection<Graph>> inputGraphGroups) {
		for (Entry<UUID, Collection<Graph>> inputGraphGroup : inputGraphGroups.entrySet()) {
			this.addInputGraphGroup(inputGraphGroup.getKey(), inputGraphGroup.getValue());
		}
	}

	/**
	 * Add a {@link Processor} this {@link Processor} depends on.
	 * 
	 * @param processor {@link Processor} this {@link Processor} depends on
	 */
	public void addInputProcessor(Processor processor);
}
