package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the <a href="https://www.w3.org/TR/annotation-vocab/">Web Annotation
 * Vocabulary</a>.
 *
 */
public class OA {
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http:// www.w3.org/ns/oa#";

	public final static Property hasBody = ResourceFactory.createProperty(namespace, "hasBody");
	public final static Property hasTarget = ResourceFactory.createProperty(namespace, "hasTarget");

}