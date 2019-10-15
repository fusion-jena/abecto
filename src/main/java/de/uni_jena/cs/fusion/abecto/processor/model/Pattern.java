package de.uni_jena.cs.fusion.abecto.processor.model;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class Pattern {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Category")
	public Resource category;
	@SparqlPattern(subject = "category", predicate = "abecto:categoryName")
	public String name;
	@SparqlPattern(subject = "category", predicate = "abecto:categoryPattern")
	public String pattern;
}
