/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

public class SparqlConstructProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// input model
		Model inputPrimaryModel = ModelFactory.createDefaultModel();
		inputPrimaryModel.add(r(1), p(1), r(2));
		inputPrimaryModel.add(r(2), p(1), r(3));
		inputPrimaryModel.add(r(3), p(1), r(4));
		inputPrimaryModel.add(r(4), p(1), r(5));
		inputPrimaryModel.add(r(5), p(1), r(6));

		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");

		// parameter
		Query query = QueryFactory
				.create("CONSTRUCT {?s <" + p(1) + "> ?o} WHERE {?s <" + p(1) + ">/<" + p(1) + "> ?o}");

		SparqlConstructProcessor processor;
		Model outputPrimaryModel;

		// test maxIterations = default ( = 1)
		processor = new SparqlConstructProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.query = query;
		processor.run();
		outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(3)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(4)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(4)));
		assertFalse(outputPrimaryModel.contains(r(2), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(2), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(3), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 1
		processor = new SparqlConstructProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.query = query;
		processor.maxIterations = 1;
		processor.run();
		outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(3)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(4)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(4)));
		assertFalse(outputPrimaryModel.contains(r(2), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(2), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(3), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 2
		processor = new SparqlConstructProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.query = query;
		processor.maxIterations = 2;
		processor.run();
		outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(3)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(5)));
		assertFalse(outputPrimaryModel.contains(r(1), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 3
		processor = new SparqlConstructProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.query = query;
		processor.maxIterations = 3;
		processor.run();
		outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(3)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(4), p(1), r(6)));

		// test maxIterations = 4 (more than needed)
		processor = new SparqlConstructProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.query = query;
		processor.maxIterations = 4;
		processor.run();
		outputPrimaryModel = processor.getOutputPrimaryModel().get();
	}

	private Resource r(int i) {
		return ResourceFactory.createResource("http://example.org/r" + i);
	}

	private Property p(int i) {
		return ResourceFactory.createProperty("http://example.org/p" + i);
	}

}
