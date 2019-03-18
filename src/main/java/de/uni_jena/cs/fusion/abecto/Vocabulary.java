package de.uni_jena.cs.fusion.abecto;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public final class Vocabulary {
	public final static Node MAPPING_PROPERTY = NodeFactory
			.createURI("http://fusion.cs.uni-jena.de/ontology/abecto/equivalent");
	public final static Node ANTI_MAPPING_PROPERTY = NodeFactory
			.createURI("http://fusion.cs.uni-jena.de/ontology/abecto/different");
	
	public final static Node RDFS_LABEL = NodeFactory
			.createURI("http://www.w3.org/2000/01/rdf-schema#label");

//	public final static Node EQUIVALENT_CLASS = NodeFactory.createURI("http://www.w3.org/2002/07/owl#equivalentClass");
//	public final static Node DISJOINT_CLASS = NodeFactory.createURI("http://www.w3.org/2002/07/owl#disjointWith");
//	public final static Node EQUIVALENT_PROPERTY = NodeFactory.createURI("http://www.w3.org/2002/07/owl#equivalentProperty");
//	public final static Node DISJOINT_PROPERTY = NodeFactory.createURI("http://www.w3.org/2002/07/owl#propertyDisjointWith");
//	public final static Node SAME_INDIVIDUAL = NodeFactory.createURI("http://www.w3.org/2002/07/owl#sameAs");
//	public final static Node DIFFERENT_INDIVIDUAL = NodeFactory.createURI("http://www.w3.org/2002/07/owl#differentFrom");

}
