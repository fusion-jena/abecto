package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface MetaProcessor extends Processor {

	/**
	 * Set the previous meta {@link RdfGraph}s.
	 * 
	 * @param mappingGraph Previous meta {@link RdfGraph}.
	 */
	public void setMetaGraphsGroup(Collection<RdfGraph> metaGraphs);

	/**
	 * Add a group of {@link RdfGraph}s to process by this processor.
	 * 
	 * @param inputGraphs {@link RdfGraph}s to process.
	 */
	public void addInputGraphsGroup(Collection<RdfGraph> inputGraphs);
}
