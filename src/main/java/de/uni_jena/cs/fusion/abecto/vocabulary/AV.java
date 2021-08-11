package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the ABECTO vocabulary.
 *
 */
public class AV {
	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://w3id.org/abecto-vocabulary#";

	public static final Property affectedAspect = ResourceFactory.createProperty(namespace, "affectedAspect");
	public static final Property affectedValue = ResourceFactory.createProperty(namespace, "affectedValue");
	public static final Property affectedVariableName = ResourceFactory.createProperty(namespace,
			"affectedVariableName");
	public static final Resource Aspect = ResourceFactory.createResource(namespace + "Aspect");
	public static final Resource AspectPattern = ResourceFactory.createResource(namespace + "AspectPattern");
	public static final Property associatedDataset = ResourceFactory.createProperty(namespace, "associatedDataset");
	public static final Property comparedToDataset = ResourceFactory.createProperty(namespace, "comparedToDataset");
	public static final Property comparedToResource = ResourceFactory.createProperty(namespace, "comparedToResource");
	public static final Property comparedToValue = ResourceFactory.createProperty(namespace, "comparedToValue");
	public static final Property containedResource = ResourceFactory.createProperty(namespace, "containedResource");
	public static final Resource CorrespondenceSet = ResourceFactory.createResource(namespace + "CorrespondenceSet");
	public static final Property definingQuery = ResourceFactory.createProperty(namespace, "definingQuery");
	public static final Property hasParameter = ResourceFactory.createProperty(namespace, "hasParameter");
	public static final Resource IncorrespondenceSet = ResourceFactory
			.createResource(namespace + "IncorrespondenceSet");
	public static final Property inputMetaDataGraph = ResourceFactory.createProperty(namespace, "inputMetaDataGraph");
	public static final Property key = ResourceFactory.createProperty(namespace, "key");
	public static final Property keyVariableName = ResourceFactory.createProperty(namespace, "keyVariableName");
	public static final Resource MetaDataGraph = ResourceFactory.createResource(namespace + "MetaDataGraph");
	public static final Property ofAspect = ResourceFactory.createProperty(namespace, "ofAspect");
	public static final Resource PrimaryDataGraph = ResourceFactory.createResource(namespace + "PrimaryDataGraph");
	public static final Property processorClass = ResourceFactory.createProperty(namespace, "processorClass");
	public static final Resource QualityAnnotationBody = ResourceFactory
			.createResource(namespace + "QualityAnnotationBody");
	public static final Resource Sparql11SelectQuery = ResourceFactory
			.createResource(namespace + "Sparql11SelectQuery");
	public static final Resource StepExecution = ResourceFactory.createResource(namespace + "StepExecution");

}