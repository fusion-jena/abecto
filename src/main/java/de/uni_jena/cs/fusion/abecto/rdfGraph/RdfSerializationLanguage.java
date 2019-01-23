package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RdfSerializationLanguage {
	// https://www.w3.org/TR/json-ld/
	JSONLD("application/ld+json", "jsonld", "JSON-LD", Pattern.compile("^\\s*\\{", Pattern.MULTILINE),
			Pattern.compile("\"@base\"\\s*:\\s*\"([^\"]+)\"")),
	// https://www.w3.org/TeamSubmission/n3/
	N3("text/n3", "n3", "N3", null, Pattern.compile("@base\\s+<([^\"]+)>")),
	// https://www.w3.org/TR/n-quads/
	NQUAD("application/n-quads", "nq", null,
			Pattern.compile("^\\s*\\S.*\\s+\\S.*\\s+\\S.*\\s+\\S.*\\s+\\.", Pattern.MULTILINE), null),
	// https://www.w3.org/TR/n-triples/
	NTRIPLES("application/n-triples", "nt", "NT", Pattern.compile("^[ \\t]*\\S.*[ \\t]s+\\S.*[ \\t]+\\S.*[ \\t]*\\.$"),
			null),
	// https://www.w3.org/TR/owl2-xml-serialization/
	// https://www.w3.org/TR/owl-ref/
	OWLXML("application/owl+xml", "owl", null,
			Pattern.compile("<Ontology\\sxmlns=\"http://www.w3.org/2002/07/owl#\"", Pattern.MULTILINE),
			Pattern.compile("xml:base=\"([^\"]+)\"")),
	// https://www.w3.org/TR/rdf-json/
	RDFJSON("application/rdf+json", "rj", "RDF/JSON", null, null),
	// https://www.w3.org/TR/rdf-syntax-grammar/
	RDFXML("application/rdf+xml", "rdf", "RDF/XML", Pattern.compile("<rdf:RDF"),
			Pattern.compile("(xml:base=\"|<owl:Ontology rdf:about=\")([^\"]+)\"")),
	// https://www.w3.org/TR/2014/REC-turtle-20140225/
	TURTLE("text/turtle", "ttl", "TTL",
			Pattern.compile("^\\s*((\\@prefix)|(\\@base)|(BASE)|(PREFIX)|(base)|(prefix))", Pattern.MULTILINE),
			Pattern.compile("@base\\s+<([^\"]+)>"));

	private final String mimeType;
	private final String fileExtension;
	private final String apacheJenaKey;
	private final Pattern languagePattern;
	private final Pattern basePattern;

	RdfSerializationLanguage(String mimeType, String fileExtension, String jena, Pattern languagePattern,
			Pattern basePattern) {
		this.mimeType = mimeType;
		this.fileExtension = fileExtension;
		this.apacheJenaKey = jena;
		this.languagePattern = languagePattern;
		this.basePattern = basePattern;
	}

	public static RdfSerializationLanguage determine(String documentStart) {
		for (RdfSerializationLanguage lang : EnumSet.allOf(RdfSerializationLanguage.class)) {
			if (Objects.nonNull(lang.getLanguagePattern()) && lang.getLanguagePattern().matcher(documentStart).find()) {
				return lang;
			}
		}
		throw new IllegalArgumentException("Unable to determine serialization language.");
	}

	public String determineBase(String documentStart) {
		if (Objects.nonNull(this.basePattern)) {
			Matcher matcher = this.basePattern.matcher(documentStart);
			matcher.find();
			return matcher.group(2);
		} else {
			throw new UnsupportedOperationException("Unable to determine base for this serialization language.");
		}
	}

	public String getApacheJenaKey() {
		return this.apacheJenaKey;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public Pattern getLanguagePattern() {
		return languagePattern;
	}

}