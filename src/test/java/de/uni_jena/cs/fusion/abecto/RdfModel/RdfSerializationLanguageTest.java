package de.uni_jena.cs.fusion.abecto.RdfModel;

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

		// TODO

		// NTRIPLES

		// TODO

		// OWLXML

		// TODO

		// RDFJSON

		// TODO

		// RDFXML

		documentStart = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">\n";
		assertEquals(RdfSerializationLanguage.RDFXML, RdfSerializationLanguage.determine(documentStart));

		documentStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<rdf:RDF xmlns:ex=\"http:/example.org/rdf#\">\n"
				+ "<owl:Ontology rdf:about=\"http:/example.org/rdf#\">\n";
		assertEquals(RdfSerializationLanguage.RDFXML, RdfSerializationLanguage.determine(documentStart));

		// TURTLE

		// TODO

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
