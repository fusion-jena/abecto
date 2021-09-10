package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class SdmxAttribute {
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://purl.org/linked-data/sdmx/2009/attribute#";

	public static final Property unitMeasure = ResourceFactory.createProperty(namespace, "unitMeasure");
}
