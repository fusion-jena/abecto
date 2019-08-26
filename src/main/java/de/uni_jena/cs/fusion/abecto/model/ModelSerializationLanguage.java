package de.uni_jena.cs.fusion.abecto.model;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ModelSerializationLanguage {
	// https://www.w3.org/TR/json-ld/
	JSONLD("application/ld+json", "jsonld", "JSON-LD", Pattern.compile("^\\s*\\{", Pattern.MULTILINE),
			Pattern.compile("\"@base\"\\s*:\\s*\"(?<base>[^\"]+)\"")),
	// https://www.w3.org/TeamSubmission/n3/
	N3("text/n3", "n3", "N3", null, Pattern.compile("@base\\s+<(?<base>[^\"]+)>")),
	// https://www.w3.org/TR/n-quads/
	NQUAD("application/n-quads", "nq", null,
			Pattern.compile("^\\s*(<\\S+>|_:\\S+)\\s+<\\S+>\\s+(<\\S+>|_:\\S+|\"[^\"]*\")\\s+(<\\S+>|_:\\S+)\\s*\\."), null),
	// https://www.w3.org/TR/n-triples/
	NTRIPLES("application/n-triples", "nt", "NT", Pattern.compile("^[ \\t]*(<\\S+>|_:\\S+)[ \\t]+(<\\S+>)[ \\t]+(<\\S+>|_:\\S+|\"\\S*\"(\\^\\^<\\S+>*|@\\S*)?)[ \\t]*\\."),
			null),
	// https://www.w3.org/TR/owl2-xml-serialization/
	// https://www.w3.org/TR/owl-ref/
	OWLXML("application/owl+xml", "owl", null,
			Pattern.compile("<Ontology\\sxmlns=\"http://www.w3.org/2002/07/owl#\"", Pattern.MULTILINE),
			Pattern.compile("xml:base=\"(?<base>[^\"]+)\"")),
	// https://www.w3.org/TR/rdf-json/
	RDFJSON("application/rdf+json", "rj", "RDF/JSON", null, null),
	// https://www.w3.org/TR/rdf-syntax-grammar/
	RDFXML("application/rdf+xml", "rdf", "RDF/XML", Pattern.compile("<rdf:RDF"),
			Pattern.compile("(xml:base=\"|<owl:Ontology rdf:about=\")(?<base>[^\\\"]+)\"")),
	// https://www.w3.org/TR/2014/REC-turtle-20140225/
	TURTLE("text/turtle", "ttl", "TTL",
			Pattern.compile("^\\s*((\\@prefix)|(\\@base)|(BASE)|(PREFIX)|(base)|(prefix))", Pattern.MULTILINE),
			Pattern.compile("@base\\s*<(?<base>[^>]+)>"));

	private final String mimeType;
	private final String fileExtension;
	private final String apacheJenaKey;
	private final Pattern languagePattern;
	private final Pattern basePattern;

	ModelSerializationLanguage(String mimeType, String fileExtension, String jena, Pattern languagePattern,
			Pattern basePattern) {
		this.mimeType = mimeType;
		this.fileExtension = fileExtension;
		this.apacheJenaKey = jena;
		this.languagePattern = languagePattern;
		this.basePattern = basePattern;
	}

	public static ModelSerializationLanguage determine(String documentStart) {
		for (ModelSerializationLanguage lang : EnumSet.allOf(ModelSerializationLanguage.class)) {
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
			return matcher.group("base");
		} else {
			return null;
		}
	}

	public String getApacheJenaKey() throws NoSuchElementException {
		if (this.apacheJenaKey == null) {
			throw new NoSuchElementException();
		}
		return this.apacheJenaKey;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public String getFileExtension() {
		return this.fileExtension;
	}

	public Pattern getLanguagePattern() {
		return this.languagePattern;
	}

}