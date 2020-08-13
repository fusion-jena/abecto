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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;

public class SparqlConstructProcessorTest {
	@Test
	public void defaultMaxIteration() throws Exception {
		String inputRdf = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
		Model inputModel = Models.read(new ByteArrayInputStream(inputRdf.getBytes()));
		SparqlConstructProcessor processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		SparqlConstructProcessor.Parameter parameter = new SparqlConstructProcessor.Parameter();
		parameter.query = "CONSTRUCT {?s <http://example.org/x> <http://example.org/y>} WHERE {?s ?p ?o.}";
		processor.setParameters(parameter);
		Model outputModel = processor.call();
		assertTrue(outputModel.contains(ResourceFactory.createResource("http://example.org/s"),
				ResourceFactory.createProperty("http://example.org/x"),
				ResourceFactory.createResource("http://example.org/y")));
	}

	@Test
	public void computeResultModel() throws Exception {
		// input model
		Model inputModel = Models.getEmptyOntModel();
		inputModel.add(r(1), p(1), r(2));
		inputModel.add(r(2), p(1), r(3));
		inputModel.add(r(3), p(1), r(4));
		inputModel.add(r(4), p(1), r(5));
		inputModel.add(r(5), p(1), r(6));
		// parameter
		SparqlConstructProcessor.Parameter parameter = new SparqlConstructProcessor.Parameter();
		parameter.query = "CONSTRUCT {?s <http://example.org/p1> ?o} WHERE {?s <http://example.org/p1>/<http://example.org/p1> ?o}";

		
		SparqlConstructProcessor processor;
		Model outputModel;

		// test maxIterations = default
		processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.setParameters(parameter);
		outputModel = processor.call();

		assertTrue(outputModel.contains(r(1), p(1), r(3)));
		assertFalse(outputModel.contains(r(1), p(1), r(4)));
		assertFalse(outputModel.contains(r(1), p(1), r(5)));
		assertFalse(outputModel.contains(r(1), p(1), r(6)));

		assertTrue(outputModel.contains(r(2), p(1), r(4)));
		assertFalse(outputModel.contains(r(2), p(1), r(5)));
		assertFalse(outputModel.contains(r(2), p(1), r(6)));

		assertTrue(outputModel.contains(r(3), p(1), r(5)));
		assertFalse(outputModel.contains(r(3), p(1), r(6)));

		assertTrue(outputModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 1
		parameter.maxIterations = 1;
		processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.setParameters(parameter);
		outputModel = processor.call();

		assertTrue(outputModel.contains(r(1), p(1), r(3)));
		assertFalse(outputModel.contains(r(1), p(1), r(4)));
		assertFalse(outputModel.contains(r(1), p(1), r(5)));
		assertFalse(outputModel.contains(r(1), p(1), r(6)));

		assertTrue(outputModel.contains(r(2), p(1), r(4)));
		assertFalse(outputModel.contains(r(2), p(1), r(5)));
		assertFalse(outputModel.contains(r(2), p(1), r(6)));

		assertTrue(outputModel.contains(r(3), p(1), r(5)));
		assertFalse(outputModel.contains(r(3), p(1), r(6)));

		assertTrue(outputModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 2
		parameter.maxIterations = 2;
		processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.setParameters(parameter);
		outputModel = processor.call();

		assertTrue(outputModel.contains(r(1), p(1), r(3)));
		assertTrue(outputModel.contains(r(1), p(1), r(4)));
		assertTrue(outputModel.contains(r(1), p(1), r(5)));
		assertFalse(outputModel.contains(r(1), p(1), r(6)));

		assertTrue(outputModel.contains(r(2), p(1), r(4)));
		assertTrue(outputModel.contains(r(2), p(1), r(5)));
		assertTrue(outputModel.contains(r(2), p(1), r(6)));

		assertTrue(outputModel.contains(r(3), p(1), r(5)));
		assertTrue(outputModel.contains(r(3), p(1), r(6)));

		assertTrue(outputModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 3
		parameter.maxIterations = 3;
		processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.setParameters(parameter);
		outputModel = processor.call();

		assertTrue(outputModel.contains(r(1), p(1), r(3)));
		assertTrue(outputModel.contains(r(1), p(1), r(4)));
		assertTrue(outputModel.contains(r(1), p(1), r(5)));
		assertTrue(outputModel.contains(r(1), p(1), r(6)));

		assertTrue(outputModel.contains(r(2), p(1), r(4)));
		assertTrue(outputModel.contains(r(2), p(1), r(5)));
		assertTrue(outputModel.contains(r(2), p(1), r(6)));

		assertTrue(outputModel.contains(r(3), p(1), r(5)));
		assertTrue(outputModel.contains(r(3), p(1), r(6)));

		assertTrue(outputModel.contains(r(4), p(1), r(6)));
		
		// test maxIterations = 4 (more than needed)
		parameter.maxIterations = 4;
		processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.setParameters(parameter);
		outputModel = processor.call();
	}

	private Resource r(int i) {
		return ResourceFactory.createResource("http://example.org/r" + i);
	}

	private Property p(int i) {
		return ResourceFactory.createProperty("http://example.org/p" + i);
	}

}
