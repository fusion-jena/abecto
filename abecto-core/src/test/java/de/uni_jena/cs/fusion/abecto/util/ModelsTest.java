/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

public class ModelsTest {

	@Test
	public void loadVeryShortOntologies() throws Exception {
		Models.read(ModelFactory.createDefaultModel(),
				new ByteArrayInputStream(("@prefix : <http://example.org/>.\n:s :p :o.").getBytes()));
	}

    @Test
    public void readUrl() throws IllegalArgumentException, IOException, InterruptedException,
            URISyntaxException {

        String content = "<http://example.org/a> <http://example.org/a> <http://example.org/a> .";
        Property resource = ResourceFactory.createProperty("http://example.org/a");

        MockWebServer mockWebServer = new MockWebServer();
        URI mockWebServerUri = new URI(mockWebServer.url("/").toString());

        // server providing proper content type
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/turtle")
                .setBody(content)
                .setResponseCode(200));
        assertTrue(Models.read(ModelFactory.createDefaultModel(), mockWebServerUri)
                .contains(resource,resource,resource));

        // server not providing proper content type
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/plain")
                .setBody(content)
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/plain")
                .setBody(content)
                .setResponseCode(200));
        assertTrue(Models.read(ModelFactory.createDefaultModel(), mockWebServerUri)
                .contains(resource,resource,resource));
    }

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(Models.getEmptyOntModel().listStatements().hasNext());
	}

}
