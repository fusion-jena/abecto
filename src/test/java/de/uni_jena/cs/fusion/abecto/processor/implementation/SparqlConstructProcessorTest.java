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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.implementation.SparqlConstructProcessor;

public class SparqlConstructProcessorTest {
	@Test
	public void testComputeResultModel() throws Exception {
		String inputRdf = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
		Model inputModel = Models.read(new ByteArrayInputStream(inputRdf.getBytes()));
		SparqlConstructProcessor processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		SparqlConstructProcessor.Parameter parameter = new SparqlConstructProcessor.Parameter();
		parameter.query = "CONSTRUCT {?s <http://example.org/x> <http://example.org/y>} WHERE {?s ?p ?o.}";
		processor.setParameters(parameter);
		Model outputModel = processor.call();
		outputModel.contains(ResourceFactory.createResource("http://example.org/s"),
				ResourceFactory.createProperty("http://example.org/x"),
				ResourceFactory.createResource("http://example.org/y"));
	}

}
