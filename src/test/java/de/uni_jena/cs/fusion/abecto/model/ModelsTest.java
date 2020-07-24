/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.model;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

public class ModelsTest {

	private static Model modelOf(String ontology, Lang lang) {
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, new ByteArrayInputStream(ontology.getBytes()), lang);
		return model;
	}

	@Test
	public void loadVeryShortOntologies() throws Exception {
		Models.read(new ByteArrayInputStream(("@prefix : <http://example.org/>.\n:s :p :o.").getBytes()));
	}

	@Test
	public void readUrl() throws IllegalArgumentException, MalformedURLException, IOException {
		WireMockServer mock = new WireMockServer(options().dynamicPort());
		mock.start();
		int port = mock.port();
		String content = "<http://example.org/a> <http://example.org/b> <http://example.org/c> .";
		mock.stubFor(get("/text/turtle").willReturn(okForContentType("text/turtle", content)));
		mock.stubFor(get("/text/plain").willReturn(okForContentType("text/plain", content)));

		// server provides proper content type
		assertTrue(Models.read(new URL("http://localhost:" + port + "/text/turtle")).contains(
				ResourceFactory.createResource("http://example.org/a"),
				ResourceFactory.createProperty("http://example.org/b"),
				ResourceFactory.createResource("http://example.org/c")));

		// server not provides proper content type
		assertTrue(Models.read(new URL("http://localhost:" + port + "/text/plain")).contains(
				ResourceFactory.createResource("http://example.org/a"),
				ResourceFactory.createProperty("http://example.org/b"),
				ResourceFactory.createResource("http://example.org/c")));

		mock.stop();
	}

	@Test
	public void readOntologyIri() {
		Resource expected = ResourceFactory.createResource("http://example.org/");
		assertEquals(expected, Models.readOntologyIri(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ."//
				, Lang.TTL)).get());
		assertEquals(expected, Models.readOntologyIri(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2004/02/skos/core#ConceptScheme> ."//
				, Lang.TTL)).get());
		assertTrue(Models.readOntologyIri(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Class> ."//
				, Lang.TTL)).isEmpty());
	}

	@Test
	public void readVersion() {
		assertEquals("2.7.3", Models.readVersion(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://www.w3.org/2002/07/owl#versionInfo> \"2.7.3\" ."//
				, Lang.TTL)).get());
	}

	@Test
	public void readVersionDateTime() {
		assertEquals("2020-07-14", Models.readVersionDateTime(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://purl.org/dc/terms/modified> \"2020-07-14\" ;"//
						+ "<http://purl.org/dc/terms/available> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/terms/created> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/terms/date> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/elements/1.1/date> \"2020-07-13\" ."//
				, Lang.TTL)).get());
		assertEquals("2020-07-14", Models.readVersionDateTime(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://purl.org/dc/terms/available> \"2020-07-14\" ;"//
						+ "<http://purl.org/dc/terms/created> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/terms/date> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/elements/1.1/date> \"2020-07-13\" ."//
				, Lang.TTL)).get());
		assertEquals("2020-07-14", Models.readVersionDateTime(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://purl.org/dc/terms/created> \"2020-07-14\" ;"//
						+ "<http://purl.org/dc/terms/date> \"2020-07-13\" ;"//
						+ "<http://purl.org/dc/elements/1.1/date> \"2020-07-13\" ."//
				, Lang.TTL)).get());
		assertEquals("2020-07-14", Models.readVersionDateTime(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://purl.org/dc/terms/date> \"2020-07-14\" ;"//
						+ "<http://purl.org/dc/elements/1.1/date> \"2020-07-13\" ."//
				, Lang.TTL)).get());
		assertEquals("2020-07-14", Models.readVersionDateTime(modelOf(//
				"<http://example.org/> a <http://www.w3.org/2002/07/owl#Ontology> ;"//
						+ "<http://purl.org/dc/elements/1.1/date> \"2020-07-14\" ."//
				, Lang.TTL)).get());
	}

	@Test
	public void readVersionIri() {
		assertEquals(ResourceFactory.createResource("http://example.org/2.7.3/"), Models.readVersionIri(modelOf(//
				"<http://example.org/> <http://www.w3.org/2002/07/owl#versionIRI> <http://example.org/2.7.3/> ."//
				, Lang.TTL)).get());
	}

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(Models.getEmptyOntModel().listStatements().hasNext());
	}

}
