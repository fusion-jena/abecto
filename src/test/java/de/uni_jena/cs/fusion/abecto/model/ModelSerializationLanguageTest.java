package de.uni_jena.cs.fusion.abecto.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ModelSerializationLanguageTest {

	@Test
	public void determine() {
		String documentStart;

		// JSONLD

		// TODO

		// N3

		// TODO

		// NQUAD

		documentStart = "<http://example.org/rdf#s> <http://example.org/rdf#p> <http://example.org/rdf#o> <http://example.org/rdf#g> .";
		assertEquals(ModelSerializationLanguage.NQUAD, ModelSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/rdf#s> <http://example.org/rdf#p> \"abc def\" <http://example.org/rdf#g> .";
		assertEquals(ModelSerializationLanguage.NQUAD, ModelSerializationLanguage.determine(documentStart));

		documentStart = "_:s1 <http://example.org/rdf#p> _:o1 _:g1 .";
		assertEquals(ModelSerializationLanguage.NQUAD, ModelSerializationLanguage.determine(documentStart));

		// NTRIPLES

		documentStart = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
		assertEquals(ModelSerializationLanguage.NTRIPLES, ModelSerializationLanguage.determine(documentStart));

		documentStart = "_:123 <http://example.org/p> \"abc\" .";
		assertEquals(ModelSerializationLanguage.NTRIPLES, ModelSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"\" .";
		assertEquals(ModelSerializationLanguage.NTRIPLES, ModelSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"abc\"^^<http://example.org/dt> .";
		assertEquals(ModelSerializationLanguage.NTRIPLES, ModelSerializationLanguage.determine(documentStart));

		documentStart = "<http://example.org/s> <http://example.org/p> \"abc\"@en .";
		assertEquals(ModelSerializationLanguage.NTRIPLES, ModelSerializationLanguage.determine(documentStart));

		// OWLXML

		// TODO

		// RDFJSON

		// TODO

		// RDFXML

		documentStart = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">";
		assertEquals(ModelSerializationLanguage.RDFXML, ModelSerializationLanguage.determine(documentStart));

		documentStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">";
		assertEquals(ModelSerializationLanguage.RDFXML, ModelSerializationLanguage.determine(documentStart));

		// TURTLE

		documentStart = "@base <http://example.org/rdf#> .\n" //
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" //
				+ ":entity rdfs:label \"label\" .";
		assertEquals(ModelSerializationLanguage.TURTLE, ModelSerializationLanguage.determine(documentStart));

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
		assertEquals("http:/example.org/rdf#", ModelSerializationLanguage.RDFXML.determineBase(documentStart));

		documentStart = "<?xml version=\"1.0\"?>\n" //
				+ "<rdf:RDF xmlns:ex=\"http:/example.org/\">\n" //
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">\n";
		assertEquals("http:/example.org/rdf#", ModelSerializationLanguage.RDFXML.determineBase(documentStart));

		// TURTLE

		documentStart = "@base <http://example.org/rdf#> .\n" //
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" //
				+ ":entity rdfs:label \"label\" .";

		assertEquals("http://example.org/rdf#", ModelSerializationLanguage.TURTLE.determineBase(documentStart));

	}

}