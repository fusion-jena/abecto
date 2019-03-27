package de.uni_jena.cs.fusion.abecto.util;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public final class Vocabulary {
	public final static Property MAPPING = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/equivalent");
	public final static Property ANTI_MAPPING = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/different");
	public final static Property CATEGORY_ASSIGNMENT_KNOWLEDGE_BASE = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryAssignmentKnowledgeBase");
	public final static Property CATEGORY_ASSIGNMENT_PATH = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryAssignmentPath");
	public final static Property CATEGORY_NAME = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryName");
	public final static Property CATEGORY_ASSIGNMENT_ENABLED = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryAssignmentEnabled");

	public final static Property RDFS_LABEL = ResourceFactory
			.createProperty("http://www.w3.org/2000/01/rdf-schema#label");

//	public final static Node EQUIVALENT_CLASS = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#equivalentClass");
//	public final static Node DISJOINT_CLASS = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#disjointWith");
//	public final static Node EQUIVALENT_PROPERTY = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#equivalentProperty");
//	public final static Node DISJOINT_PROPERTY = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#propertyDisjointWith");
//	public final static Node SAME_INDIVIDUAL = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#sameAs");
//	public final static Node DIFFERENT_INDIVIDUAL = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#differentFrom");

}
