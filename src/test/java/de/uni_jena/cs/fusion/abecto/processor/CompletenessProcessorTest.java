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
package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.metaentity.Measurement;
import de.uni_jena.cs.fusion.abecto.metaentity.Omission;
import de.uni_jena.cs.fusion.abecto.processor.CompletenessProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Models;

public class CompletenessProcessorTest {

	@Test
	public void computeResultModel() throws Exception {

		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		UUID ontologyId3 = UUID.randomUUID();

		String inputRdf1 = ""//
				+ "<http://example.org/111> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/112> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/113> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/114> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/121> <http://example.org/p2> <http://example.org/o2> ." //
				+ "<http://example.org/122> <http://example.org/p2> <http://example.org/o2> ." //
				+ "<http://example.org/123> <http://example.org/p2> <http://example.org/o2> ." //
				+ "<http://example.org/124> <http://example.org/p2> <http://example.org/o2> .";
		String inputRdf2 = ""//
				+ "<http://example.org/211> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/212> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/221> <http://example.org/p2> <http://example.org/o2> ." //
				+ "<http://example.org/2211> <http://example.org/p2> <http://example.org/o2> ." //
				+ "<http://example.org/222> <http://example.org/p2> <http://example.org/o2> .";
		String inputRdf3 = ""//
				+ "<http://example.org/315> <http://example.org/p1> <http://example.org/o1> ." //
				+ "<http://example.org/325> <http://example.org/p2> <http://example.org/o2> .";
		Model inputModel1 = Models.read(new ByteArrayInputStream(inputRdf1.getBytes()));
		Model inputModel2 = Models.read(new ByteArrayInputStream(inputRdf2.getBytes()));
		Model inputModel3 = Models.read(new ByteArrayInputStream(inputRdf3.getBytes()));

		Model inputMetaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(Arrays.asList(//
				of(111, 211), //
				of(112, 212), //
				of(121, 221), //
				of(121, 2211), //
				of(122, 222), //
				of(221, 2211)), inputMetaModel);

		String category1 = "c1";
		String category2 = "c2";
		String pattern1 = "{?c1 <http://example.org/p1> <http://example.org/o1>}";
		String pattern2 = "{?c2 <http://example.org/p2> <http://example.org/o2>}";
		SparqlEntityManager.insert(Arrays.asList(//
				new Category(category1, pattern1, ontologyId1), //
				new Category(category1, pattern1, ontologyId2), //
				new Category(category1, pattern1, ontologyId3), //
				new Category(category2, pattern2, ontologyId1), //
				new Category(category2, pattern2, ontologyId2), //
				new Category(category2, pattern2, ontologyId3)), inputMetaModel);

		CompletenessProcessor processor = new CompletenessProcessor();
		processor.addInputModelGroup(ontologyId1, Collections.singleton(inputModel1));
		processor.addInputModelGroup(ontologyId2, Collections.singleton(inputModel2));
		processor.addInputModelGroup(ontologyId3, Collections.singleton(inputModel3));
		processor.addMetaModels(Collections.singleton(inputMetaModel));
		processor.setParameters(new CompletenessProcessor.Parameter());
		Model outputModel = processor.call();

		// check absolute coverage
		assertEquals(2L, absoluteCoverate(category1, ontologyId1, ontologyId2, outputModel).get());
		assertEquals(3L, absoluteCoverate(category2, ontologyId1, ontologyId2, outputModel).get());
		assertEquals(2L, absoluteCoverate(category1, ontologyId2, ontologyId1, outputModel).get());
		assertEquals(2L, absoluteCoverate(category2, ontologyId2, ontologyId1, outputModel).get());

		assertEquals(0L, absoluteCoverate(category1, ontologyId1, ontologyId3, outputModel).get());
		assertEquals(0L, absoluteCoverate(category2, ontologyId1, ontologyId3, outputModel).get());
		assertEquals(0L, absoluteCoverate(category1, ontologyId3, ontologyId1, outputModel).get());
		assertEquals(0L, absoluteCoverate(category2, ontologyId3, ontologyId1, outputModel).get());

		assertEquals(0L, absoluteCoverate(category1, ontologyId2, ontologyId3, outputModel).get());
		assertEquals(0L, absoluteCoverate(category2, ontologyId2, ontologyId3, outputModel).get());
		assertEquals(0L, absoluteCoverate(category1, ontologyId3, ontologyId2, outputModel).get());
		assertEquals(0L, absoluteCoverate(category2, ontologyId3, ontologyId2, outputModel).get());

		assertEquals(12, SparqlEntityManager
				.select(new Measurement(null, null, "Coverage (absolute)", null, null, null, null, null), outputModel)
				.size());

		// check relative coverage
		assertEquals(100L, relativeCoverate(category1, ontologyId1, ontologyId2, outputModel).get());
		assertEquals(100L, relativeCoverate(category2, ontologyId1, ontologyId2, outputModel).get());
		assertEquals(50L, relativeCoverate(category1, ontologyId2, ontologyId1, outputModel).get());
		assertEquals(50L, relativeCoverate(category2, ontologyId2, ontologyId1, outputModel).get());

		assertEquals(0L, relativeCoverate(category1, ontologyId1, ontologyId3, outputModel).get());
		assertEquals(0L, relativeCoverate(category2, ontologyId1, ontologyId3, outputModel).get());
		assertEquals(0L, relativeCoverate(category1, ontologyId3, ontologyId1, outputModel).get());
		assertEquals(0L, relativeCoverate(category2, ontologyId3, ontologyId1, outputModel).get());

		assertEquals(0L, relativeCoverate(category1, ontologyId2, ontologyId3, outputModel).get());
		assertEquals(0L, relativeCoverate(category2, ontologyId2, ontologyId3, outputModel).get());
		assertEquals(0L, relativeCoverate(category1, ontologyId3, ontologyId2, outputModel).get());
		assertEquals(0L, relativeCoverate(category2, ontologyId3, ontologyId2, outputModel).get());

		assertEquals(12,
				SparqlEntityManager
						.select(new Measurement(null, null, "Coverage (relative in %)", null, null, null, null, null),
								outputModel)
						.size());

		// check omissions
		assertTrue(omission(category1, ontologyId1, ontologyId2, 113, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId1, ontologyId2, 114, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId2, 123, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId2, 124, outputModel).isPresent());

		assertTrue(omission(category1, ontologyId1, ontologyId3, 111, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId1, ontologyId3, 112, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId1, ontologyId3, 113, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId1, ontologyId3, 114, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId3, ontologyId1, 315, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId3, 121, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId3, 122, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId3, 123, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId1, ontologyId3, 124, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId3, ontologyId1, 325, outputModel).isPresent());

		assertTrue(omission(category1, ontologyId2, ontologyId3, 211, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId2, ontologyId3, 212, outputModel).isPresent());
		assertTrue(omission(category1, ontologyId3, ontologyId2, 315, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId2, ontologyId3, 221, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId2, ontologyId3, 2211, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId2, ontologyId3, 222, outputModel).isPresent());
		assertTrue(omission(category2, ontologyId3, ontologyId2, 325, outputModel).isPresent());

		assertEquals(21, SparqlEntityManager.select(new Omission(null, null, null, null, null), outputModel).size());

		// check duplicates
		assertTrue(duplicate(ontologyId2, 221, outputModel).isPresent());
		assertTrue(duplicate(ontologyId2, 2211, outputModel).isPresent());

		assertEquals(2, SparqlEntityManager.select(new Issue(), outputModel).size());

		// TODO check completeness
		
		// TODO check parameter use
	}

	private static Optional<Long> absoluteCoverate(String categoryName, UUID ontologyId1, UUID ontologyId2, Model model)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		Optional<Measurement> o = SparqlEntityManager.selectOne(
				new Measurement(null, ontologyId1, "Coverage (absolute)", null, Optional.of("of category"),
						Optional.of(categoryName), Optional.of("in ontology"), Optional.of(ontologyId2.toString())),
				model);
		if (o.isPresent()) {
			return Optional.of(o.get().value);
		} else {
			return Optional.empty();
		}
	}

	private static Optional<Long> relativeCoverate(String categoryName, UUID ontologyId1, UUID ontologyId2, Model model)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		Optional<Measurement> o = SparqlEntityManager.selectOne(
				new Measurement(null, ontologyId1, "Coverage (relative in %)", null, Optional.of("of category"),
						Optional.of(categoryName), Optional.of("in ontology"), Optional.of(ontologyId2.toString())),
				model);
		if (o.isPresent()) {
			return Optional.of(o.get().value);
		} else {
			return Optional.empty();
		}
	}

	private static Optional<Omission> omission(String categoryName, UUID ontologyId1, UUID ontologyId2, int resourceId,
			Model model) throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		return SparqlEntityManager.selectOne(new Omission(null, categoryName, ontologyId2,
				ResourceFactory.createResource("http://example.org/" + resourceId), ontologyId1), model);
	}

	private static Optional<Issue> duplicate(UUID ontologyId1, int resourceId, Model model)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		return SparqlEntityManager.selectOne(new Issue(null, ontologyId1,
				ResourceFactory.createResource("http://example.org/" + resourceId), "Duplicated Resource", null),
				model);
	}

	private static Mapping of(int resourceId1, int resourceId2) {
		return Mapping.of(ResourceFactory.createResource("http://example.org/" + resourceId1),
				ResourceFactory.createResource("http://example.org/" + resourceId2));
	}

}
