package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the <a href="https://www.w3.org/TR/vocab-dqv/">Data Quality
 * Vocabulary</a>.
 *
 */
public class DQV {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://www.w3.org/ns/dqv#";

	public final static Property computedOn = ResourceFactory
			.createProperty("http://www.w3.org/ns/dqv#computedOn");
}
