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
package de.uni_jena.cs.fusion.abecto.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

public class ModelsTest {

	@Test
	public void loadVeryShortOntologies() throws Exception {
		Models.read(ModelFactory.createDefaultModel(),
				new ByteArrayInputStream(("@prefix : <http://example.org/>.\n:s :p :o.").getBytes()));
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
		assertTrue(Models.read(ModelFactory.createDefaultModel(), new URL("http://localhost:" + port + "/text/turtle"))
				.contains(ResourceFactory.createResource("http://example.org/a"),
						ResourceFactory.createProperty("http://example.org/b"),
						ResourceFactory.createResource("http://example.org/c")));

		// server not provides proper content type
		assertTrue(Models.read(ModelFactory.createDefaultModel(), new URL("http://localhost:" + port + "/text/plain"))
				.contains(ResourceFactory.createResource("http://example.org/a"),
						ResourceFactory.createProperty("http://example.org/b"),
						ResourceFactory.createResource("http://example.org/c")));

		mock.stop();
	}

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(Models.getEmptyOntModel().listStatements().hasNext());
	}

}
