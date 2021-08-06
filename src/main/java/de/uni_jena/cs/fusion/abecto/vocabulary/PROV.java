package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the <a href="https://www.w3.org/TR/prov-overview/">PROV-O</a>
 * vocabulary.
 *
 */
public class PROV {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://www.w3.org/ns/prov#";
	public final static Property startedAtTime = ResourceFactory
			.createProperty("http://www.w3.org/ns/prov#startedAtTime");
	public final static Property endedAtTime = ResourceFactory.createProperty("http://www.w3.org/ns/prov#endedAtTime");
	public final static Property wasGeneratedBy = ResourceFactory
			.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy");
	public final static Property used = ResourceFactory.createProperty("http://www.w3.org/ns/prov#used");
}
