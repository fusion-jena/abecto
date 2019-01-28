package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface TransformationProcessor extends Processor {

	/**
	 * Set the {@link RdfGraph}s to process by this processor.
	 * 
	 * @param sources {@link RdfGraph}s to process.
	 */
	public void setSources(Collection<RdfGraph> sources);
}
