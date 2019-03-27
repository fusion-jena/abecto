package de.uni_jena.cs.fusion.abecto.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.path.Path;

public class SparqlUtil {
	private final static Prologue PROLOGUE = new Prologue();

	static {
		PROLOGUE.setPrefix("owl", "http://www.w3.org/2002/07/owl#");
		PROLOGUE.setPrefix("prov", "http://www.w3.org/ns/prov#");
		PROLOGUE.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		PROLOGUE.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		PROLOGUE.setPrefix("schema", "http://schema.org/");
		PROLOGUE.setPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		PROLOGUE.setPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
	}
	
	public static Path parsePath(String pattern) throws ParseException {
		InputStream stream = new ByteArrayInputStream(pattern.getBytes());
		SPARQLParser11 parser = new SPARQLParser11(stream);
		parser.setPrologue(PROLOGUE);
		return parser.Path();
	}
	
	public static String pathToString(Path path) {
		return path.toString(PROLOGUE);
	}
	
	public static String sanitizePath(String pattern) throws ParseException {
		return pathToString(parsePath(pattern));
	}

}
