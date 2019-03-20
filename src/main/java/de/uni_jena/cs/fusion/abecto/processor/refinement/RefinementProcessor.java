package de.uni_jena.cs.fusion.abecto.processor.refinement;

import java.util.Collection;

import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.processor.Processor;

public interface RefinementProcessor extends Processor {

	/**
	 * Add the previous meta {@link Graph}.
	 * 
	 * @param metaGraph previous meta {@link Graph}
	 */
	public void addMetaGraph(Graph metaGraph);

	/**
	 * Add a {@link Graph} to process.
	 * 
	 * @param inputGraph {@link Graph}s to process
	 */
	public void addInputGraph(Graph inputGraph);

	/**
	 * Add {@link Graph}s to process.
	 * 
	 * @param inputGraphGroups {@link Graph}s to process
	 */
	default public void addInputGraph(Collection<Graph> inputGraphGroups) {
		for (Graph inputGraphGroup : inputGraphGroups) {
			this.addInputGraph(inputGraphGroup);
		}
	}

	/**
	 * Add a {@link Processor} this {@link Processor} depends on.
	 * 
	 * @param processor {@link Processor} this {@link Processor} depends on
	 */
	public void addDependetProcessor(Processor processor);
}
