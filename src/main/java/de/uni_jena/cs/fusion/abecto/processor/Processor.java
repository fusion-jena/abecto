package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.concurrent.Callable;

import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public interface Processor extends Callable<RdfGraph> {

	/**
	 * Returns a map of allowed properties for this processor and the required data
	 * type of the parameter
	 * 
	 * @return Map of allowed properties and the required data type.
	 */
	public Map<String, Class<?>> getPropertyTypes();

	/**
	 * Set the properties for this processor. Earlier property configurations will
	 * be overwritten.
	 * 
	 * @param properties {@link Map} of property keys and property values.
	 */
	public void setProperties(Map<String, Object> properties);

	/**
	 * Set the {@link ProgressListener} for this processor.
	 * 
	 * @param listener {@link ProgressListener} to use.
	 */
	public void setListener(ProgressListener listener);
}
