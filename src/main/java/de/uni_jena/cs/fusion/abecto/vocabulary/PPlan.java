package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the
 * <a href="http://vocab.linkeddata.es/p-plan/index.html">P-Plan</a> vocabulary.
 *
 */
public class PPlan {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://purl.org/net/p-plan#";

	public final static Resource Plan = ResourceFactory.createResource("http://purl.org/net/p-plan#Plan");
	public final static Property isStepOfPlan = ResourceFactory
			.createProperty("http://purl.org/net/p-plan#isStepOfPlan");
	public final static Property isPrecededBy = ResourceFactory
			.createProperty("http://purl.org/net/p-plan#isPrecededBy");
	public final static Property correspondsToStep = ResourceFactory
			.createProperty("http://purl.org/net/p-plan#correspondsToStep");
}
