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
package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class CorrespondencesTest {
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

	@Test
	public void correspondentOrIncorrespondent() {
		Model model = ModelFactory.createDefaultModel();
		model.add(resource1, AV.correspondsToResource, resource2);
		model.add(resource2, AV.correspondsToResource, resource1);
		model.add(resource2, AV.correspondsNotToResource, resource3);
		model.add(resource3, AV.correspondsNotToResource, resource2);

		assertTrue(Correspondences.correspondentOrIncorrespondent(resource1, resource1, model));
		assertTrue(Correspondences.correspondentOrIncorrespondent(resource1, resource2, model));
		assertFalse(Correspondences.correspondentOrIncorrespondent(resource1, resource3, model));

		assertTrue(Correspondences.correspondentOrIncorrespondent(resource2, resource1, model));
		assertTrue(Correspondences.correspondentOrIncorrespondent(resource2, resource2, model));
		assertTrue(Correspondences.correspondentOrIncorrespondent(resource2, resource3, model));

		assertFalse(Correspondences.correspondentOrIncorrespondent(resource3, resource1, model));
		assertTrue(Correspondences.correspondentOrIncorrespondent(resource3, resource2, model));
		assertTrue(Correspondences.correspondentOrIncorrespondent(resource3, resource3, model));
	}

	@Test
	public void correspond() {
		Model model = ModelFactory.createDefaultModel();
		model.add(resource1, AV.correspondsToResource, resource2);
		model.add(resource2, AV.correspondsToResource, resource1);
		model.add(resource2, AV.correspondsNotToResource, resource3);
		model.add(resource3, AV.correspondsNotToResource, resource2);

		assertTrue(Correspondences.correspond(resource1, resource1, model));
		assertTrue(Correspondences.correspond(resource1, resource2, model));
		assertFalse(Correspondences.correspond(resource1, resource3, model));

		assertTrue(Correspondences.correspond(resource2, resource1, model));
		assertTrue(Correspondences.correspond(resource2, resource2, model));
		assertFalse(Correspondences.correspond(resource2, resource3, model));

		assertFalse(Correspondences.correspond(resource3, resource1, model));
		assertFalse(Correspondences.correspond(resource3, resource2, model));
		assertTrue(Correspondences.correspond(resource3, resource3, model));
	}

	@Test
	public void anyIncorrespondend() {
		Model model = ModelFactory.createDefaultModel();
		model.add(resource1, AV.correspondsToResource, resource2);
		model.add(resource1, AV.correspondsToResource, resource3);
		model.add(resource1, AV.correspondsNotToResource, resource4);

		model.add(resource2, AV.correspondsToResource, resource1);
		model.add(resource2, AV.correspondsToResource, resource3);
		model.add(resource2, AV.correspondsNotToResource, resource4);

		model.add(resource3, AV.correspondsToResource, resource1);
		model.add(resource3, AV.correspondsToResource, resource2);
		model.add(resource3, AV.correspondsNotToResource, resource4);

		model.add(resource4, AV.correspondsNotToResource, resource1);
		model.add(resource4, AV.correspondsNotToResource, resource2);
		model.add(resource4, AV.correspondsNotToResource, resource3);

		assertFalse(Correspondences.anyIncorrespondend(model));

		assertFalse(Correspondences.anyIncorrespondend(model, resource1));
		assertFalse(Correspondences.anyIncorrespondend(model, resource2));
		assertFalse(Correspondences.anyIncorrespondend(model, resource3));
		assertFalse(Correspondences.anyIncorrespondend(model, resource4));

		assertFalse(Correspondences.anyIncorrespondend(model, resource1, resource1));
		assertFalse(Correspondences.anyIncorrespondend(model, resource1, resource2));
		assertFalse(Correspondences.anyIncorrespondend(model, resource1, resource3));
		assertTrue(Correspondences.anyIncorrespondend(model, resource1, resource4));

		assertFalse(Correspondences.anyIncorrespondend(model, resource2, resource1));
		assertFalse(Correspondences.anyIncorrespondend(model, resource2, resource2));
		assertFalse(Correspondences.anyIncorrespondend(model, resource2, resource3));
		assertTrue(Correspondences.anyIncorrespondend(model, resource2, resource4));

		assertFalse(Correspondences.anyIncorrespondend(model, resource3, resource1));
		assertFalse(Correspondences.anyIncorrespondend(model, resource3, resource2));
		assertFalse(Correspondences.anyIncorrespondend(model, resource3, resource3));
		assertTrue(Correspondences.anyIncorrespondend(model, resource3, resource4));

		assertTrue(Correspondences.anyIncorrespondend(model, resource4, resource1));
		assertTrue(Correspondences.anyIncorrespondend(model, resource4, resource2));
		assertTrue(Correspondences.anyIncorrespondend(model, resource4, resource3));
		assertFalse(Correspondences.anyIncorrespondend(model, resource4, resource4));

		assertFalse(Correspondences.anyIncorrespondend(model, resource1, resource2, resource3));
		assertTrue(Correspondences.anyIncorrespondend(model, resource1, resource2, resource4));
		assertTrue(Correspondences.anyIncorrespondend(model, resource1, resource3, resource4));
		assertTrue(Correspondences.anyIncorrespondend(model, resource2, resource3, resource4));

		assertTrue(Correspondences.anyIncorrespondend(model, resource1, resource2, resource3, resource4));
	}

	@Test
	public void allCorrespondend() {
		Model model = ModelFactory.createDefaultModel();
		model.add(resource1, AV.correspondsToResource, resource2);
		model.add(resource1, AV.correspondsToResource, resource3);
		model.add(resource1, AV.correspondsNotToResource, resource4);

		model.add(resource2, AV.correspondsToResource, resource1);
		model.add(resource2, AV.correspondsToResource, resource3);
		model.add(resource2, AV.correspondsNotToResource, resource4);

		model.add(resource3, AV.correspondsToResource, resource1);
		model.add(resource3, AV.correspondsToResource, resource2);
		model.add(resource3, AV.correspondsNotToResource, resource4);

		model.add(resource4, AV.correspondsNotToResource, resource1);
		model.add(resource4, AV.correspondsNotToResource, resource2);
		model.add(resource4, AV.correspondsNotToResource, resource3);

		assertTrue(Correspondences.allCorrespondend(model));

		assertTrue(Correspondences.allCorrespondend(model, resource1));
		assertTrue(Correspondences.allCorrespondend(model, resource2));
		assertTrue(Correspondences.allCorrespondend(model, resource3));
		assertTrue(Correspondences.allCorrespondend(model, resource4));

		assertTrue(Correspondences.allCorrespondend(model, resource1, resource1));
		assertTrue(Correspondences.allCorrespondend(model, resource1, resource2));
		assertTrue(Correspondences.allCorrespondend(model, resource1, resource3));
		assertFalse(Correspondences.allCorrespondend(model, resource1, resource4));

		assertTrue(Correspondences.allCorrespondend(model, resource2, resource1));
		assertTrue(Correspondences.allCorrespondend(model, resource2, resource2));
		assertTrue(Correspondences.allCorrespondend(model, resource2, resource3));
		assertFalse(Correspondences.allCorrespondend(model, resource2, resource4));

		assertTrue(Correspondences.allCorrespondend(model, resource3, resource1));
		assertTrue(Correspondences.allCorrespondend(model, resource3, resource2));
		assertTrue(Correspondences.allCorrespondend(model, resource3, resource3));
		assertFalse(Correspondences.allCorrespondend(model, resource3, resource4));

		assertFalse(Correspondences.allCorrespondend(model, resource4, resource1));
		assertFalse(Correspondences.allCorrespondend(model, resource4, resource2));
		assertFalse(Correspondences.allCorrespondend(model, resource4, resource3));
		assertTrue(Correspondences.allCorrespondend(model, resource4, resource4));

		assertTrue(Correspondences.allCorrespondend(model, resource1, resource2, resource3));
		assertFalse(Correspondences.allCorrespondend(model, resource1, resource2, resource4));
		assertFalse(Correspondences.allCorrespondend(model, resource1, resource3, resource4));
		assertFalse(Correspondences.allCorrespondend(model, resource2, resource3, resource4));

		assertFalse(Correspondences.allCorrespondend(model, resource1, resource2, resource3, resource4));
	}

	@Test
	public void addCorrespondence() {
		Model inputModel = ModelFactory.createDefaultModel();
		Model outputModel = ModelFactory.createDefaultModel();

		Correspondences.addCorrespondence(inputModel, outputModel, aspect1);
		assertTrue(outputModel.isEmpty());

		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1);
		assertTrue(outputModel.isEmpty());

		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1, resource2);
		assertTrue(Correspondences.allCorrespondend(outputModel, resource1, resource2));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource1));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource2));
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		outputModel.removeAll();

		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1, resource2, resource3);
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource1));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource2));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource3));
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		assertFalse(outputModel.contains(resource3, AV.correspondsToResource, resource3));
		assertTrue(Correspondences.allCorrespondend(outputModel, resource1, resource2, resource3));
		outputModel.removeAll();

		// collection
		Correspondences.addCorrespondence(inputModel, outputModel, aspect1,
				Arrays.asList(resource1, resource2, resource3));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource1));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource2));
		assertTrue(outputModel.contains(aspect1, AV.relevantResource, resource3));
		assertFalse(outputModel.contains(resource1, AV.correspondsToResource, resource1));
		assertFalse(outputModel.contains(resource2, AV.correspondsToResource, resource2));
		assertFalse(outputModel.contains(resource3, AV.correspondsToResource, resource3));
		assertTrue(Correspondences.allCorrespondend(outputModel, resource1, resource2, resource3));
		outputModel.removeAll();

		// assert statements not inserted again
		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource1, resource2);
		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1, resource2, resource3);
		assertFalse(Correspondences.allCorrespondend(outputModel, resource1, resource2, resource3));
		assertTrue(Correspondences.allCorrespondend(outputModel, resource1, resource3));
		assertTrue(Correspondences.allCorrespondend(outputModel, resource2, resource3));
		inputModel.removeAll();
		outputModel.removeAll();

		// assert nothing insert if all exist
		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource1, resource2, resource3);
		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1, resource2, resource3);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();

		// assert contradictions not inserted
		inputModel.add(resource1, AV.correspondsNotToResource, resource2);
		inputModel.add(resource2, AV.correspondsNotToResource, resource1);
		Correspondences.addCorrespondence(inputModel, outputModel, aspect1, resource1, resource2, resource3);
		assertTrue(outputModel.isEmpty());
	}

	@Test
	public void addIncorrespondence() {
		Model inputModel = ModelFactory.createDefaultModel();
		Model outputModel = ModelFactory.createDefaultModel();

		Correspondences.addIncorrespondence(inputModel, outputModel, aspect1, resource1, resource2);
		assertTrue(Correspondences.anyIncorrespondend(outputModel, resource1, resource2));
		outputModel.removeAll();

		// assert statements not inserted again
		Correspondences.addIncorrespondence(inputModel, inputModel, aspect1, resource1, resource2);
		Correspondences.addIncorrespondence(inputModel, outputModel, aspect1, resource1, resource2);
		assertTrue(outputModel.isEmpty());
		inputModel.removeAll();
		outputModel.removeAll();

		// assert contradictions not inserted
		inputModel.add(resource1, AV.correspondsToResource, resource2);
		inputModel.add(resource2, AV.correspondsToResource, resource1);
		Correspondences.addIncorrespondence(inputModel, outputModel, aspect1, resource1, resource2);
		assertTrue(outputModel.isEmpty());
	}

	@Test
	public void getCorrespondenceSets() {
		Model inputModel = ModelFactory.createDefaultModel();

		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource1, resource2, resource3);
		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource4, resource5);
		Correspondences.addCorrespondence(inputModel, inputModel, aspect2, resource6, resource7);

		List<List<Resource>> correspondenceSets = Correspondences.getCorrespondenceSets(inputModel, aspect1)
				.collect(Collectors.toList());
		assertEquals(2, correspondenceSets.size());
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
			default:
				fail("Resource \"" + resourceBase + min + "\" found as first resource in set.");
			}
		}

	}

	@Test
	public void getCorrespondingResources() {
		Model inputModel = ModelFactory.createDefaultModel();

		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource1, resource2, resource3);
		Correspondences.addCorrespondence(inputModel, inputModel, aspect1, resource4, resource5);
		Correspondences.addCorrespondence(inputModel, inputModel, aspect2, resource6, resource7);

		assertArrayEquals(new Resource[] { resource2, resource3 },
				Correspondences.getCorrespondingResources(inputModel, aspect1, resource1)
						.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));
		assertArrayEquals(new Resource[] { resource1, resource3 },
				Correspondences.getCorrespondingResources(inputModel, aspect1, resource2)
						.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));
		assertArrayEquals(new Resource[] { resource1, resource2 },
				Correspondences.getCorrespondingResources(inputModel, aspect1, resource3)
						.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));

		assertArrayEquals(new Resource[] { resource5 },
				Correspondences.getCorrespondingResources(inputModel, aspect1, resource4)
						.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));
		assertArrayEquals(new Resource[] { resource4 },
				Correspondences.getCorrespondingResources(inputModel, aspect1, resource5)
						.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));

		assertArrayEquals(new Resource[] {}, Correspondences.getCorrespondingResources(inputModel, aspect1, resource6)
				.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));
		assertArrayEquals(new Resource[] {}, Correspondences.getCorrespondingResources(inputModel, aspect1, resource7)
				.sorted((a, b) -> a.getURI().compareTo(b.getURI())).toArray(l -> new Resource[l]));

	}

}
