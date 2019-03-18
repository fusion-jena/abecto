package de.uni_jena.cs.fusion.abecto.rdfGraph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfSerializationLanguage;

public class RdfSerializationLanguageTest {

	@Test
	public void determine() {
		String documentStart;

		// JSONLD

		// TODO

		// N3

		// TODO

		// NQUAD

		documentStart = "<http://example.org/s> <http://example.org/#p> <http://example.org/o> <http://example.org/g> .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));
		
		documentStart = "<http://example.org/s> <http://example.org/#p> \"abc def\" <http://example.org/g> .\r\n" + 
				"_:s1 <http://example.org/#p> _:o1 _:g1 .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));
		
		documentStart = "_:s1 <http://example.org/#p> _:o1 _:g1 .";
		assertEquals(RdfSerializationLanguage.NQUAD, RdfSerializationLanguage.determine(documentStart));

		// NTRIPLES

		// TODO

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

		documentStart = "@base <http://example.org/> .\n" //
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

		documentStart = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xml:base=\"http:/example.org/rdf#\">\n";
		assertEquals("http:/example.org/rdf#", RdfSerializationLanguage.RDFXML.determineBase(documentStart));

		documentStart = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">\n";
		assertEquals("http:/example.org/rdf#", RdfSerializationLanguage.RDFXML.determineBase(documentStart));

		// TURTLE

		// TODO

	}

}
