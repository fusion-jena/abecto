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

package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class MappingProcessorTest {
	private static class DummyMappingProcessor extends MappingProcessor<DummyMappingProcessor> {
		@Override
		public void mapDatasets(Resource dataset1, Resource dataset2) {
			// do nothing
		}
	}
	static Model inputModel;
	static Model outputModel;
	static DummyMappingProcessor processor;
	String resourceBase = "http://example.org/resource";
	Resource resource1 = ResourceFactory.createResource(resourceBase + "1");
	Resource resource2 = ResourceFactory.createResource(resourceBase + "2");
	Resource resource3 = ResourceFactory.createResource(resourceBase + "3");
	Resource resource4 = ResourceFactory.createResource(resourceBase + "4");
	Resource resource5 = ResourceFactory.createResource(resourceBase + "5");
	Resource resource6 = ResourceFactory.createResource(resourceBase + "6");

	Resource resource7 = ResourceFactory.createResource(resourceBase + "7");

	@Test
	public void addCorrespondence() {
		processor.addCorrespondence();
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();

		processor.addCorrespondence(resource1);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();

		processor.addCorrespondence(resource1, resource2);
		assertTrue(processor.allCorrespondend(resource1, resource2));
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		inputModel.removeAll();
		outputModel.removeAll();

		processor.addCorrespondence(resource1, resource2, resource3);
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		assertFalse(outputModel.contains(resource3, AV.correspondsToResource, resource3));
		assertTrue(processor.allCorrespondend(resource1, resource2, resource3));
		inputModel.removeAll();
		outputModel.removeAll();

		// collection
		processor.addCorrespondence(Arrays.asList(resource1, resource2, resource3));
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		assertFalse(outputModel.contains(resource3, AV.correspondsToResource, resource3));
		assertTrue(processor.allCorrespondend(resource1, resource2, resource3));
		inputModel.removeAll();
		outputModel.removeAll();

		// collection and array
		processor.addCorrespondence(Arrays.asList(resource1), resource2, resource3);
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		assertFalse(outputModel.contains(resource3, AV.correspondsToResource, resource3));
		assertTrue(processor.allCorrespondend(resource1, resource2, resource3));
		inputModel.removeAll();
		outputModel.removeAll();

		// assert nothing insert if all exist
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource1, AV.correspondsToResource, resource3);
		processor.addCorrespondence(resource1, resource2, resource3);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();

		// assert contradictions not inserted
		inputModel.add(resource1, AV.correspondsNotToResource, resource2);
		inputModel.add(resource2, AV.correspondsNotToResource, resource1);
		processor.addCorrespondence(resource1, resource2, resource3);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();
	}

	@Test
	public void addIncorrespondence() {
		processor.addIncorrespondence(resource1, resource2);
		assertTrue(processor.anyIncorrespondend(resource1, resource2));
		outputModel.removeAll();

		// assert statements not inserted again
		inputModel.add(resource1, AV.correspondsNotToResource, resource2);
		processor.addIncorrespondence(resource1, resource2);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();

		// assert contradictions not inserted
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		processor.addIncorrespondence(resource1, resource2);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();
	}

	@Test
	public void correspondentOrIncorrespondent() {
		// check, if implicit assignments have been inferred

		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource2, AV.correspondsNotToResource, resource3);
		inputModel.add(resource3, AV.correspondsNotToResource, resource4);

		assertTrue(processor.correspondentOrIncorrespondent(resource1, resource1));
		assertTrue(processor.correspondentOrIncorrespondent(resource1, resource2));
		assertTrue(processor.correspondentOrIncorrespondent(resource1, resource3)); // implicit
		assertFalse(processor.correspondentOrIncorrespondent(resource1, resource4));

		assertTrue(processor.correspondentOrIncorrespondent(resource2, resource1)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource2, resource2));
		assertTrue(processor.correspondentOrIncorrespondent(resource2, resource3));
		assertFalse(processor.correspondentOrIncorrespondent(resource2, resource4));

		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource1)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource2)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource3));
		assertTrue(processor.correspondentOrIncorrespondent(resource3, resource4));

		assertFalse(processor.correspondentOrIncorrespondent(resource4, resource1));
		assertFalse(processor.correspondentOrIncorrespondent(resource4, resource2));
		assertTrue(processor.correspondentOrIncorrespondent(resource4, resource3)); // implicit
		assertTrue(processor.correspondentOrIncorrespondent(resource4, resource4));
	}

	@Test
	public void getCorrespondenceGroup() {
		processor.addCorrespondence(resource1, resource2, resource3);
		processor.addCorrespondence(resource4, resource5);

		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource1)),
				new HashSet<>(Arrays.asList(resource1, resource2, resource3)));
		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource2)),
				new HashSet<>(Arrays.asList(resource1, resource2, resource3)));
		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource3)),
				new HashSet<>(Arrays.asList(resource1, resource2, resource3)));
		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource4)),
				new HashSet<>(Arrays.asList(resource4, resource5)));
		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource5)),
				new HashSet<>(Arrays.asList(resource4, resource5)));
		assertEquals(new HashSet<>(processor.getCorrespondenceGroup(resource6)),
				new HashSet<>(Arrays.asList(resource6)));
	}

	@Test
	public void getCorrespondenceGroups() {
		processor.addCorrespondence(resource1, resource2, resource3);
		processor.addCorrespondence(resource4, resource5);
		processor.addCorrespondence(resource6, resource7);

		List<List<Resource>> correspondenceSets = processor.getCorrespondenceGroups()
				.collect(Collectors.toList());
		assertEquals(3, correspondenceSets.size());
		for (List<Resource> correspondenceSet : correspondenceSets) {
			int min = correspondenceSet.stream()
					.mapToInt(r -> Integer.parseInt(r.getURI().substring(resourceBase.length()))).min().getAsInt();
			switch (min) {
				case 1:
					assertArrayEquals(new Resource[] { resource1, resource2, resource3 }, correspondenceSet.toArray());
					break;
				case 4:
					assertArrayEquals(new Resource[] { resource4, resource5 }, correspondenceSet.toArray());
					break;
				case 6:
					assertArrayEquals(new Resource[] { resource6, resource7 }, correspondenceSet.toArray());
					break;
			default:
				fail("Resource \"" + resourceBase + min + "\" found as first resource in set.");
			}
		}
	}

	@Test
	public void inferences() {
		// assert correspondence inverse statement inference
		processor.addCorrespondence(resource1, resource2);
		assertTrue(processor.allCorrespondend(resource2, resource1));
		outputModel.removeAll();

		// assert correspondence transitive statement inference (insert order 1)
		processor.addCorrespondence(resource1, resource2);
		processor.addCorrespondence(resource2, resource3);
		assertTrue(processor.allCorrespondend(resource1, resource3));
		outputModel.removeAll();

		// assert correspondence transitive statement inference (insert order 2)
		processor.addCorrespondence(resource2, resource3);
		processor.addCorrespondence(resource1, resource2);
		assertTrue(processor.allCorrespondend(resource1, resource3));
		outputModel.removeAll();

		// assert incorrespondence inverse statement inference
		processor.addIncorrespondence(resource1, resource2);
		assertTrue(processor.anyIncorrespondend(resource2, resource1));
		outputModel.removeAll();

		// assert incorrespondence-correspondence chain inference (insert order 1)
		processor.addCorrespondence(resource1, resource2);
		processor.addIncorrespondence(resource2, resource3);
		assertTrue(processor.anyIncorrespondend(resource1, resource3));
		outputModel.removeAll();

		// assert incorrespondence-correspondence chain inference (insert order 2)
		processor.addIncorrespondence(resource2, resource3);
		processor.addCorrespondence(resource1, resource2);
		assertTrue(processor.anyIncorrespondend(resource1, resource3));
		outputModel.removeAll();
	}

	@Test
	public void inferTransitiveCorrespondences() {
		Model model = ModelFactory.createDefaultModel();
		model.add(resource1, AV.correspondsToResource, resource2);
		MappingProcessor.inferTransitiveCorrespondences(model);
		assertTrue(model.contains(resource2, AV.correspondsToResource, resource1));
	}

	@BeforeEach
	public void reset() {
		processor = new DummyMappingProcessor();
		inputModel = ModelFactory.createDefaultModel();
		processor.addInputMetaModel(null, inputModel);
		outputModel = processor.getOutputMetaModel(null);
	}
}
