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

	public final static Resource Plan = ResourceFactory.createResource(namespace + "Plan");
	public final static Property isStepOfPlan = ResourceFactory.createProperty(namespace, "isStepOfPlan");
	public final static Property isPrecededBy = ResourceFactory.createProperty(namespace, "isPrecededBy");
	public final static Property correspondsToStep = ResourceFactory.createProperty(namespace, "correspondsToStep");
}