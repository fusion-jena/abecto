package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RdfSerializationLanguage {
	// https://www.w3.org/TR/json-ld/
	JSONLD("application/ld+json", "jsonld", "JSON-LD", Pattern.compile("^\\s*\\{", Pattern.MULTILINE)),
	// https://www.w3.org/TeamSubmission/n3/
	N3("text/n3", "n3", "N3", null),
	// https://www.w3.org/TR/n-quads/
	NQUAD("application/n-quads", "nq", null, Pattern.compile("^\\s*\\S.*\\s+\\S.*\\s+\\S.*\\s+\\S.*\\s+\\.", Pattern.MULTILINE)),
	// https://www.w3.org/TR/n-triples/
	NTRIPLES("application/n-triples", "nt", "NT", Pattern.compile("^[ \\t]*\\S.*[ \\t]s+\\S.*[ \\t]+\\S.*[ \\t]*\\.$")),
	// https://www.w3.org/TR/owl2-xml-serialization/
	// https://www.w3.org/TR/owl-ref/
	OWLXML("application/owl+xml", "owl", null,
			Pattern.compile("<Ontology\\sxmlns=\"http://www.w3.org/2002/07/owl#\"", Pattern.MULTILINE)),
	// https://www.w3.org/TR/rdf-json/
	RDFJSON("application/rdf+json", "rj", "RDF/JSON", null),
	// https://www.w3.org/TR/rdf-syntax-grammar/
	RDFXML("application/rdf+xml", "rdf", "RDF/XML", Pattern.compile("<rdf:RDF")),
	// https://www.w3.org/TR/2014/REC-turtle-20140225/
	TURTLE("text/turtle", "ttl", "TTL",
			Pattern.compile("^\\s*((\\@prefix)|(\\@base)|(BASE)|(PREFIX)|(base)|(prefix))", Pattern.MULTILINE));

	public static RdfSerializationLanguage determine(String documentStart) {
		for (RdfSerializationLanguage lang : EnumSet.allOf(RdfSerializationLanguage.class)) {
			if (Objects.nonNull(lang.getMagicPattern()) && lang.getMagicPattern().matcher(documentStart).find()) {
				return lang;
			}
		}
		throw new IllegalArgumentException("Unable to determine serialization language.");
	}

	private final String jena;
	private final Pattern magicPattern;

	RdfSerializationLanguage(String mimeType, String fileExtension, String jena, Pattern magicPattern) {
		this.magicPattern = magicPattern;
		this.jena = jena;
	}

	public String determineBase(String documentStart) {
		Pattern pattern;
		switch (this) {
		case JSONLD:
			// "@base": "http://example.com/document.jsonld"
			pattern = Pattern.compile("\"@base\"\\s*:\\s*\"([^\"]+)\"");
			break;
		case N3:
			pattern = Pattern.compile("@base\\s+<([^\"]+)>");
			break;
		case NQUAD:
			return null;
		case NTRIPLES:
			return null;
		case OWLXML:
			// xml:base="http://example.org/here/"
			pattern = Pattern.compile("xml:base=\"([^\"]+)\"");
			break;
		case RDFJSON:
			return null;
		case RDFXML:
			// xml:base="http://example.org/here/"
			// <owl:Ontology rdf:about="http://example.org/here/"
			pattern = Pattern.compile("(xml:base=\"|<owl:Ontology rdf:about=\")([^\"]+)\"");
			break;
		case TURTLE:
			// @base <http://example.org/> .
			pattern = Pattern.compile("@base\\s+<([^\"]+)>");
			break;
		default:
			return null;
		}
		Matcher matcher = pattern.matcher(documentStart);
		matcher.find();
		return matcher.group(2);
	}

	public Pattern getMagicPattern() {
		return this.magicPattern;
	}

	public String apacheJenaKey() {
		return this.jena;
	}

}