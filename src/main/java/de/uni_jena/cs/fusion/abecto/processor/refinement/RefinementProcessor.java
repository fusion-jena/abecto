package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface RefinementProcessor extends Processor {

	/**
	 * Add the previous meta {@link RdfGraph}s.
	 * 
	 * @param metaGraphs {@link RdfGraph}s to use as previous meta results.
	 */
	public void addMetaGraphs(Collection<RdfGraph> metaGraphs);

	/**
	 * Add a group of {@link RdfGraph}s to process.
	 * 
	 * @param inputGraphGroup Group of {@link RdfGraph}s to process.
	 */
	public void addInputGraphGroup(Collection<RdfGraph> inputGraphGroup);

	/**
	 * Add groups of {@link RdfGraph}s to process.
	 * 
	 * @param inputGraphGroups Group of {@link RdfGraph}s to process.
	 */
	default public void addInputGraphsGroups(Collection<Collection<RdfGraph>> inputGraphGroups) {
		for (Collection<RdfGraph> inputGraphGroup : inputGraphGroups) {
			this.addInputGraphGroup(inputGraphGroup);
		}
	}
}
