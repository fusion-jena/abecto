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

	public static final Property affectedAspect = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#affectedAspect");
	public static final Property affectedValue = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#affectedValue");
	public static final Property affectedVariableName = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#affectedVariableName");
	public static final Resource Aspect = ResourceFactory.createResource("http://w3id.org/abecto-vocabulary#Aspect");
	public static final Resource AspectPattern = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#AspectPattern");
	public static final Property associatedDataset = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#associatedDataset");
	public static final Property comparedToDataset = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#comparedToDataset");
	public static final Property comparedToResource = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#comparedToResource");
	public static final Property comparedToValue = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#comparedToValue");
	public static final Property containedResource = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#containedResource");
	public static final Resource CorrespondenceSet = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#CorrespondenceSet");
	public static final Property definingQuery = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#definingQuery");
	public static final Property hasParameter = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#hasParameter");
	public static final Resource IncorrespondenceSet = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#IncorrespondenceSet");
	public static final Property inputMetaDataGraph = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#inputMetaDataGraph");
	public static final Property key = ResourceFactory.createProperty("http://w3id.org/abecto-vocabulary#key");
	public static final Property keyVariableName = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#keyVariableName");
	public static final Resource MetaDataGraph = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#MetaDataGraph");
	public static final Property ofAspect = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#ofAspect");
	public static final Resource PrimaryDataGraph = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#PrimaryDataGraph");
	public static final Property processorClass = ResourceFactory
			.createProperty("http://w3id.org/abecto-vocabulary#processorClass");
	public static final Resource QualityAnnotationBody = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#QualityAnnotationBody");
	public static final Resource Sparql11SelectQuery = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#Sparql11SelectQuery");
	public static final Resource StepExecution = ResourceFactory
			.createResource("http://w3id.org/abecto-vocabulary#StepExecution");

}