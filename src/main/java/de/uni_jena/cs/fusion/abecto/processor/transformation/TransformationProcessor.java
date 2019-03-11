package de.uni_jena.cs.fusion.abecto.processor.transformation;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface TransformationProcessor extends Processor {

	/**
	 * Set the {@link RdfGraph}s to process by this processor.
	 * 
	 * @param inputGraphs {@link RdfGraph}s to process.
	 */
	public void setInputGraphs(Collection<RdfGraph> inputGraphs);
}
