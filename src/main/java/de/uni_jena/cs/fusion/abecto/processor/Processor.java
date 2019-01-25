package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.concurrent.Callable;

import de.uni_jena.cs.fusion.abecto.progress.NotifyingProgress;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface Processor extends Callable<RdfGraph>, NotifyingProgress {

	public Map<String, Class<?>> getPropertyTypes();

	public void setProperties(Map<String, Object> properties);
}
