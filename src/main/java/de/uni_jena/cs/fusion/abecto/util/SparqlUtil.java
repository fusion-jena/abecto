package de.uni_jena.cs.fusion.abecto.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.*;

public class SparqlUtil {
	private final static Prologue PROLOGUE = new Prologue();

	static {
		PROLOGUE.setPrefix("owl", OWL2.getURI());
		PROLOGUE.setPrefix("prov", "http://www.w3.org/ns/prov#");
		PROLOGUE.setPrefix("rdf", RDF.getURI());
		PROLOGUE.setPrefix("rdfs", RDFS.getURI());
		PROLOGUE.setPrefix("schema", "http://schema.org/");
		PROLOGUE.setPrefix("skos", SKOS.getURI());
		PROLOGUE.setPrefix("xsd", XSD.getURI());
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
