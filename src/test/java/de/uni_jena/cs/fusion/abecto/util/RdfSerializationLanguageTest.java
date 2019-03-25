package de.uni_jena.cs.fusion.abecto.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.util.RdfSerializationLanguage;

public class RdfSerializationLanguageTest {

	@Test
	public void determine() {
		String documentStart;

		// JSONLD

		// TODO

		// N3

		// TODO

		// NQUAD

		documentStart = "<http://example.org/rdf#s> <http://example.org/rdf#p> <http://example.org/rdf#o> <http://example.org/rdf#g> .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/rdf#s> <http://example.org/rdf#p> \"abc def\" <http://example.org/rdf#g> .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));

		documentStart = "_:s1 <http://example.org/rdf#p> _:o1 _:g1 .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));

		// NTRIPLES

		documentStart = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
		assertEquals(RdfSerializationLanguage.NTRIPLES, RdfSerializationLanguage.determine(documentStart));

		documentStart = "_:123 <http://example.org/p> \"abc\" .";
		assertEquals(RdfSerializationLanguage.NTRIPLES, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"\" .";
		assertEquals(RdfSerializationLanguage.NTRIPLES, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"abc\"^^<http://example.org/dt> .";
		assertEquals(RdfSerializationLanguage.NTRIPLES, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"abc\"@en .";
		assertEquals(RdfSerializationLanguage.NTRIPLES, RdfSerializationLanguage.determine(documentStart));

		// OWLXML

		// TODO

		// RDFJSON

		// TODO

		// RDFXML

		documentStart = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">";
		assertEquals(RdfSerializationLanguage.RDFXML, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">";
		assertEquals(RdfSerializationLanguage.RDFXML, RdfSerializationLanguage.determine(documentStart));

		// TURTLE

		documentStart = "@base <http://example.org/rdf#> .\n" //
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" //
				+ ":entity rdfs:label \"label\" .";
		assertEquals(RdfSerializationLanguage.TURTLE, RdfSerializationLanguage.determine(documentStart));

	}

	@Test
	public void determineBase() {
		String documentStart;

		// JSONLD

		// TODO

		// N3

		// TODO

		// NQUAD

		// TODO

		// NTRIPLES

		// TODO

		// OWLXML

		// TODO

		// RDFJSON

		// TODO

		// RDFXML

		documentStart = "<?xml version=\"1.0\"?>\n" //
				+ "<rdf:RDF xml:base=\"http:/example.org/rdf#\">\n";
		assertEquals("http:/example.org/rdf#", RdfSerializationLanguage.RDFXML.determineBase(documentStart));

		documentStart = "<?xml version=\"1.0\"?>\n" //
				+ "<rdf:RDF xmlns:ex=\"http:/example.org/\">\n" //
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">\n";
		assertEquals("http:/example.org/rdf#", RdfSerializationLanguage.RDFXML.determineBase(documentStart));

		// TURTLE

		documentStart = "@base <http://example.org/rdf#> .\n" //
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" //
				+ ":entity rdfs:label \"label\" .";

		assertEquals("http://example.org/rdf#", RdfSerializationLanguage.TURTLE.determineBase(documentStart));

	}

}
