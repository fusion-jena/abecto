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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class ProcessorTest {
	String resourceBase = "http://example.org/resource";
	Resource resource1 = ResourceFactory.createResource(resourceBase + "1");
	Resource resource2 = ResourceFactory.createResource(resourceBase + "2");
	Resource resource3 = ResourceFactory.createResource(resourceBase + "3");
	Resource resource4 = ResourceFactory.createResource(resourceBase + "4");
	Resource resource5 = ResourceFactory.createResource(resourceBase + "5");
	Resource resource6 = ResourceFactory.createResource(resourceBase + "6");
	Resource resource7 = ResourceFactory.createResource(resourceBase + "7");
	Resource aspect1 = ResourceFactory.createResource("http://example.org/aspect1");
	Resource aspect2 = ResourceFactory.createResource("http://example.org/aspect2");
	Resource aspectBlankNode = ResourceFactory.createResource();

	private static class DummyProcessor extends Processor<DummyProcessor> {
		@Override
		public void run() {
			// do nothing
		}
	}

	static Model inputModel;
	static Model outputModel;
	static DummyProcessor processor;

	@BeforeEach
	public void reset() {
		processor = new DummyProcessor();
		inputModel = ModelFactory.createDefaultModel();
		processor.addInputMetaModel(null, inputModel);
		outputModel = processor.getOutputMetaModel(null);
	}

	@Test
	public void correspondentOrIncorrespondent() {
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource2, AV.correspondsNotToResource, resource3);
		inputModel.add(resource3, AV.correspondsNotToResource, resource4);

		assertTrue(processor.correspondentOrIncorrespondent(resource1, resource1));
		assertTrue(processor.correspondentOrIncorrespondent(resource1, resource2));
		assertFalse(processor.correspondentOrIncorrespondent(resource1, resource3)); // implicit
		assertFalse(processor.correspondentOrIncorrespondent(resource1, resource4));

		assertFalse(processor.correspondentOrIncorrespondent(resource2, resource1)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource2, resource2));
		assertTrue(processor.correspondentOrIncorrespondent(resource2, resource3));
		assertFalse(processor.correspondentOrIncorrespondent(resource2, resource4));

		assertFalse(processor.correspondentOrIncorrespondent(resource3, resource1)); // implicit
		assertFalse(processor.correspondentOrIncorrespondent(resource3, resource2)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource3));
		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource4));

		assertFalse(processor.correspondentOrIncorrespondent(resource4, resource1));
		assertFalse(processor.correspondentOrIncorrespondent(resource4, resource2));
		assertFalse(processor.correspondentOrIncorrespondent(resource4, resource3)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource4, resource4));
	}

	@Test
	public void correspond() {
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource2, AV.correspondsToResource, resource1);
		inputModel.add(resource2, AV.correspondsNotToResource, resource3);
		inputModel.add(resource3, AV.correspondsNotToResource, resource2);

		assertTrue(processor.correspond(resource1, resource1));
		assertTrue(processor.correspond(resource1, resource2));
		assertFalse(processor.correspond(resource1, resource3));

		assertTrue(processor.correspond(resource2, resource1));
		assertTrue(processor.correspond(resource2, resource2));
		assertFalse(processor.correspond(resource2, resource3));

		assertFalse(processor.correspond(resource3, resource1));
		assertFalse(processor.correspond(resource3, resource2));
		assertTrue(processor.correspond(resource3, resource3));
	}

	@Test
	public void anyIncorrespondend() {
		inputModel.add(resource1, AV.correspondsNotToResource, resource4);
		inputModel.add(resource2, AV.correspondsNotToResource, resource4);
		inputModel.add(resource3, AV.correspondsNotToResource, resource4);

		inputModel.add(resource4, AV.correspondsNotToResource, resource1);
		inputModel.add(resource4, AV.correspondsNotToResource, resource2);
		inputModel.add(resource4, AV.correspondsNotToResource, resource3);

		assertFalse(processor.anyIncorrespondend());

		assertFalse(processor.anyIncorrespondend(resource1));
		assertFalse(processor.anyIncorrespondend(resource2));
		assertFalse(processor.anyIncorrespondend(resource3));
		assertFalse(processor.anyIncorrespondend(resource4));

		assertFalse(processor.anyIncorrespondend(resource1, resource1));
		assertFalse(processor.anyIncorrespondend(resource1, resource2));
		assertFalse(processor.anyIncorrespondend(resource1, resource3));
		assertTrue(processor.anyIncorrespondend(resource1, resource4));

		assertFalse(processor.anyIncorrespondend(resource2, resource1));
		assertFalse(processor.anyIncorrespondend(resource2, resource2));
		assertFalse(processor.anyIncorrespondend(resource2, resource3));
		assertTrue(processor.anyIncorrespondend(resource2, resource4));

		assertFalse(processor.anyIncorrespondend(resource3, resource1));
		assertFalse(processor.anyIncorrespondend(resource3, resource2));
		assertFalse(processor.anyIncorrespondend(resource3, resource3));
		assertTrue(processor.anyIncorrespondend(resource3, resource4));

		assertTrue(processor.anyIncorrespondend(resource4, resource1));
		assertTrue(processor.anyIncorrespondend(resource4, resource2));
		assertTrue(processor.anyIncorrespondend(resource4, resource3));
		assertFalse(processor.anyIncorrespondend(resource4, resource4));

		assertFalse(processor.anyIncorrespondend(resource1, resource2, resource3));
		assertTrue(processor.anyIncorrespondend(resource1, resource2, resource4));
		assertTrue(processor.anyIncorrespondend(resource1, resource3, resource4));
		assertTrue(processor.anyIncorrespondend(resource2, resource3, resource4));

		assertTrue(processor.anyIncorrespondend(resource1, resource2, resource3, resource4));
	}

	@Test
	public void allCorrespondend() {
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource1, AV.correspondsToResource, resource3);
		inputModel.add(resource1, AV.correspondsNotToResource, resource4);

		inputModel.add(resource2, AV.correspondsToResource, resource1);
		inputModel.add(resource2, AV.correspondsToResource, resource3);
		inputModel.add(resource2, AV.correspondsNotToResource, resource4);

		inputModel.add(resource3, AV.correspondsToResource, resource1);
		inputModel.add(resource3, AV.correspondsToResource, resource2);
		inputModel.add(resource3, AV.correspondsNotToResource, resource4);

		inputModel.add(resource4, AV.correspondsNotToResource, resource1);
		inputModel.add(resource4, AV.correspondsNotToResource, resource2);
		inputModel.add(resource4, AV.correspondsNotToResource, resource3);

		assertTrue(processor.allCorrespondend());

		assertTrue(processor.allCorrespondend(resource1));
		assertTrue(processor.allCorrespondend(resource2));
		assertTrue(processor.allCorrespondend(resource3));
		assertTrue(processor.allCorrespondend(resource4));

		assertTrue(processor.allCorrespondend(resource1, resource1));
		assertTrue(processor.allCorrespondend(resource1, resource2));
		assertTrue(processor.allCorrespondend(resource1, resource3));
		assertFalse(processor.allCorrespondend(resource1, resource4));

		assertTrue(processor.allCorrespondend(resource2, resource1));
		assertTrue(processor.allCorrespondend(resource2, resource2));
		assertTrue(processor.allCorrespondend(resource2, resource3));
		assertFalse(processor.allCorrespondend(resource2, resource4));

		assertTrue(processor.allCorrespondend(resource3, resource1));
		assertTrue(processor.allCorrespondend(resource3, resource2));
		assertTrue(processor.allCorrespondend(resource3, resource3));
		assertFalse(processor.allCorrespondend(resource3, resource4));

		assertFalse(processor.allCorrespondend(resource4, resource1));
		assertFalse(processor.allCorrespondend(resource4, resource2));
		assertFalse(processor.allCorrespondend(resource4, resource3));
		assertTrue(processor.allCorrespondend(resource4, resource4));

		assertTrue(processor.allCorrespondend(resource1, resource2, resource3));
		assertFalse(processor.allCorrespondend(resource1, resource2, resource4));
		assertFalse(processor.allCorrespondend(resource1, resource3, resource4));
		assertFalse(processor.allCorrespondend(resource2, resource3, resource4));

		assertFalse(processor.allCorrespondend(resource1, resource2, resource3, resource4));
	}
}
