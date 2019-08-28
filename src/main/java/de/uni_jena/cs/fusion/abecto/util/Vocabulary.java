package de.uni_jena.cs.fusion.abecto.util;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public final class Vocabulary {
	public final static Property MAPPING = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/equivalent");
	public final static Property ANTI_MAPPING = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/different");
	public final static Property KNOWLEDGE_BASE = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/knowledgeBase");
	public final static Property RELATION_ASSIGNMENT_PATH = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/relationTypeAssignmentPath");
	public final static Property RELATION_TYPE_NAME = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/relationTypeName");
	public final static Property RELATION_TYPE_ASSIGNMENT_ENABLED = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/relationTypeAssignmentEnabled");
	public final static Property VALUE = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/value");

	public final static Resource CATEGORY = ResourceFactory
			.createResource("http://fusion.cs.uni-jena.de/ontology/abecto/Category");
	public final static Resource COUNT_MEASURE = ResourceFactory
			.createResource("http://fusion.cs.uni-jena.de/ontology/abecto/CountMeasure");
	
	public final static Property CATEGORY_NAME = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryName");
	public final static Property CATEGORY_TEMPLATE = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryTemplate");
	public final static Property CATEGORY_TARGET = ResourceFactory
			.createProperty("http://fusion.cs.uni-jena.de/ontology/abecto/categoryTarget");

//	public final static Node EQUIVALENT_CLASS = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#equivalentClass");
//	public final static Node DISJOINT_CLASS = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#disjointWith");
//	public final static Node EQUIVALENT_PROPERTY = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#equivalentProperty");
//	public final static Node DISJOINT_PROPERTY = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#propertyDisjointWith");
//	public final static Node SAME_INDIVIDUAL = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#sameAs");
//	public final static Node DIFFERENT_INDIVIDUAL = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#differentFrom");

}
