package de.uni_jena.cs.fusion.abecto.model;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods to determine the language and base of RDF serializations and
 * constants for serialization languages.
 */
public enum ModelSerializationLanguage {
	/**
	 * The JSON-based Serialization for Linked Data as defined in
	 * <a href="https://www.w3.org/TR/json-ld/">https://www.w3.org/TR/json-ld/</a>.
	 */
	JSONLD("application/ld+json", "jsonld", "JSON-LD", Pattern.compile("^\\s*\\{", Pattern.MULTILINE),
			Pattern.compile("\"@base\"\\s*:\\s*\"(?<base>[^\"]+)\"")),
	/**
	 * The N3 Serialization as defined in <a href=
	 * "https://www.w3.org/TeamSubmission/n3/">https://www.w3.org/TeamSubmission/n3/</a>.
	 */
	N3("text/n3", "n3", "N3", null, Pattern.compile("@base\\s+<(?<base>[^\"]+)>")),
	/**
	 * The N-Quads Serialization as defined in
	 * <a href="https://www.w3.org/TR/n-quads/">https://www.w3.org/TR/n-quads/</a>.
	 */
	NQUADS("application/n-quads", "nq", null,
			Pattern.compile("^\\s*(<\\S+>|_:\\S+)\\s+<\\S+>\\s+(<\\S+>|_:\\S+|\"[^\"]*\")\\s+(<\\S+>|_:\\S+)\\s*\\."),
			null),
	/**
	 * The N-Triples Serialization as defined in <a href=
	 * "https://www.w3.org/TR/n-triples/">https://www.w3.org/TR/n-triples/</a>.
	 */
	NTRIPLES("application/n-triples", "nt", "NT", Pattern.compile(
			"^[ \\t]*(<\\S+>|_:\\S+)[ \\t]+(<\\S+>)[ \\t]+(<\\S+>|_:\\S+|\"\\S*\"(\\^\\^<\\S+>*|@\\S*)?)[ \\t]*\\."),
			null),
	/**
	 * The OWL 2 Web Ontology Language XML Serialization as defined in
	 * <a href="https://www.w3.org/TR/owl-ref/">https://www.w3.org/TR/owl-ref/</a>
	 * and <a href=
	 * "https://www.w3.org/TR/owl2-xml-serialization/">https://www.w3.org/TR/owl2-xml-serialization/</a>.
	 */
	OWLXML("application/owl+xml", "owl", null,
			Pattern.compile("<Ontology\\sxmlns=\"http://www.w3.org/2002/07/owl#\"", Pattern.MULTILINE),
			Pattern.compile("xml:base=\"(?<base>[^\"]+)\"")),
	/**
	 * The JSON Alternate Serialization as defined in <a href=
	 * "https://www.w3.org/TR/rdf-json/">https://www.w3.org/TR/rdf-json/</a>.
	 */
	RDFJSON("application/rdf+json", "rj", "RDF/JSON", null, null),
	/**
	 * The RDF XML Serialization as defined in <a href=
	 * "https://www.w3.org/TR/rdf-syntax-grammar/">https://www.w3.org/TR/rdf-syntax-grammar/</a>.
	 */
	RDFXML("application/rdf+xml", "rdf", "RDF/XML", Pattern.compile("<rdf:RDF"),
			Pattern.compile("(xml:base=\"|<owl:Ontology rdf:about=\")(?<base>[^\\\"]+)\"")),
	/**
	 * The Turtle Serialization as defined in <a href=
	 * "https://www.w3.org/TR/2014/REC-turtle-20140225/">https://www.w3.org/TR/2014/REC-turtle-20140225/</a>.
	 */
	TURTLE("text/turtle", "ttl", "TTL",
			// https://regex101.com/r/UTj2JG/4
			// TODO allow comments inside of statements
			Pattern.compile(
					"^\\s*(((@prefix|@PREFIX)\\s+\\S+:\\s+<\\S+>|(@base|@BASE)\\s+\\<\\S+\\>|\\S+\\s+\\S+\\s+\\S+(\\s+\\,\\s+\\S+)*(\\s+\\;\\s+\\S+\\s+\\S+(\\s+\\,\\s+\\S+)*)*)\\s*\\.\\s*|\\#.*\\n|PREFIX\\s+\\S+:\\s+<\\S+>\\s*|BASE\\s+\\<\\S+\\>\\s*)+",
					Pattern.MULTILINE),
			Pattern.compile("(@base|@BASE|BASE)\\s+<(?<base>[^>]+)>"));

	/** MIME-Type of this serialization language */
	private final String mimeType;
	/** file extension of this serialization language */
	private final String fileExtension;
	/** String representation of this serialization language in Apache Jena */
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
			if (Objects.nonNull(lang.languagePattern) && lang.languagePattern.matcher(documentStart).find()) {
				return lang;
			}
		}
		throw new IllegalArgumentException("Unable to determine serialization language.");
	}

	public String determineBase(String documentStart) {
		if (Objects.nonNull(this.basePattern)) {
			Matcher matcher = this.basePattern.matcher(documentStart);
			if (matcher.find()) {
				return matcher.group("base");
			} else {
				return null;
			}
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
}