package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface MappingProcessor extends Processor {

	public final static Node MAPPING_PROPERTY = NodeFactory.createURI("http://fusion.cs.uni-jena.de/ontology/abecto/mappedTo");
	public final static Node ANTI_MAPPING_PROPERTY = NodeFactory.createURI("http://fusion.cs.uni-jena.de/ontology/abecto/notMappedTo");

	/**
	 * Add a group {@link RdfGraph}s to process by this processor.
	 * 
	 * @param sources {@link RdfGraph}s to process.
	 */
	public void addSourcesGroup(Collection<RdfGraph> sources);

	/**
	 * Set the {@link RdfGraph}s of known mappings and anti-mappings.
	 * 
	 * @param mappingGraph {@link RdfGraph} of all known mappings and anti-mappings.
	 */
	public void setMappings(Collection<RdfGraph> mappings);
}
