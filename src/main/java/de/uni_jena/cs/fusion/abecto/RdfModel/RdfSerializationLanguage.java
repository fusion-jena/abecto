package de.uni_jena.cs.fusion.abecto.RdfModel;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rdfhdt.hdt.enums.RDFNotation;

public enum RdfSerializationLanguage {
	JSONLD("JSON-LD", RDFNotation.JSONLD, Pattern.compile("^\\s*\\{", Pattern.MULTILINE)),
	N3("N3", RDFNotation.N3, null),
	NQUAD(null, RDFNotation.NQUAD, Pattern.compile("^\\s*\\S.*\\s+\\S.*\\s+\\S.*\\s+\\S.*\\s+\\.", Pattern.MULTILINE)),
	NTRIPLES("NT", RDFNotation.NTRIPLES, Pattern.compile("^[ \\t]*\\S.*[ \\t]s+\\S.*[ \\t]+\\S.*[ \\t]*\\.$")),
	OWLXML(null, null, Pattern.compile("<Ontology\\sxmlns=\"http://www.w3.org/2002/07/owl#\"", Pattern.MULTILINE)),
	RDFJSON("RDF/JSON", null, null),
	RDFXML("RDF/XML", RDFNotation.RDFXML, Pattern.compile("<rdf:RDF")),
	TURTLE("TTL", RDFNotation.TURTLE,
			Pattern.compile("^\\s*((\\@prefix)|(\\@base)|(BASE)|(PREFIX)|(base)|(prefix))", Pattern.MULTILINE));

	public static RdfSerializationLanguage determine(String documentStart) {
		for (RdfSerializationLanguage lang : EnumSet.allOf(RdfSerializationLanguage.class)) {
			if (Objects.nonNull(lang.getMagicPattern()) && lang.getMagicPattern().matcher(documentStart).find()) {
				return lang;
			}
		}
		throw new IllegalArgumentException("Unable to determine serialization language.");
	}

	private final RDFNotation hdt;
	private final String jena;
	private final Pattern magicPattern;

	RdfSerializationLanguage(String jena, RDFNotation hdt, Pattern magicPattern) {
		this.magicPattern = magicPattern;
		this.jena = jena;
		this.hdt = hdt;
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

	public RDFNotation hdtKey() {
		return this.hdt;
	}

	public String jenaKey() {
		return this.jena;
	}

}