package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class OM {
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://www.ontology-of-units-of-measure.org/resource/om-2/";

	public static final Resource one = ResourceFactory.createResource(namespace + "one");

	public static String getURI() {
		return namespace;
	}
}
