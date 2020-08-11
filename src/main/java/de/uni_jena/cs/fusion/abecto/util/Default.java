package de.uni_jena.cs.fusion.abecto.util;

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

public class Default {
	public final static Prologue PROLOGUE = new Prologue();

	static {
		PROLOGUE.setPrefix("owl", OWL2.getURI());
		PROLOGUE.setPrefix("prov", "http://www.w3.org/ns/prov#");
		PROLOGUE.setPrefix("rdf", RDF.getURI());
		PROLOGUE.setPrefix("rdfs", RDFS.getURI());
		PROLOGUE.setPrefix("schema", "http://schema.org/");
		PROLOGUE.setPrefix("skos", SKOS.getURI());
		PROLOGUE.setPrefix("xsd", XSD.getURI());
	}
}
