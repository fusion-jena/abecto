package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface SubsequentProcessor extends Processor {

	public void setSources(Collection<RdfGraph> sources);
}
