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

import static de.uni_jena.cs.fusion.abecto.TestUtil.aspect;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;

public class JaroWinklerMappingProcessorTest {
	@BeforeAll
	public static void initJena() {
		// ensure Jena initialization
		JenaSystem.init();
	}

	Query pattern = QueryFactory.create("SELECT ?key ?label WHERE {?key <" + RDFS.label + "> ?label .}");
	Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
	Aspect aspect2 = new Aspect(aspect(2), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);

	@Test
	public void useSelectedAspect() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDFS.label, "abcdabcdabcdabcdabcd")
				.add(resource("entity2"), RDFS.label, "efghefghefghefghefgh")
				.add(resource("entity3"), RDFS.label, "ijklijklijklijklijkl");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource("entity4"), RDFS.label, "abcdabcdabcdabcdabcd")
				.add(resource("entity5"), RDFS.label, "efghefghefghefghabcd")
				.add(resource("entity6"), RDFS.label, "mnopmnopmnopmnopmnop");
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2)
				.addAspects(aspect1, aspect2);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertTrue(processor.allCorrespondend(resource("entity1"), resource("entity4")));
		assertTrue(processor.allCorrespondend(resource("entity2"), resource("entity5")));
		assertEquals(2, processor.getCorrespondenceGroups(aspect(1)).count());
		assertEquals(0, processor.getCorrespondenceGroups(aspect(2)).count());
	}

	@Test
	public void handelOptionalValue() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDF.type, OWL.Thing)//
				.add(resource("entity2"), RDF.type, OWL.Thing)//
				.add(resource("entity2"), RDFS.label, "def");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource("entity3"), RDF.type, OWL.Thing)//
				.add(resource("entity3"), RDFS.label, "abc")//
				.add(resource("entity4"), RDF.type, OWL.Thing);

		Query pattern = QueryFactory.create(""//
				+ "SELECT ?key ?label WHERE {"//
				+ "  ?key <" + RDF.type + "> <" + OWL.Thing + "> ."//
				+ "  OPTIONAL {?key <" + RDFS.label + "> ?label}"//
				+ "}");
		Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertEquals(0, processor.getCorrespondenceGroups(aspect(1)).count());
	}

	@Test
	public void handelEmptyModels() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDFS.label, "abc").add(resource("entity2"), RDFS.label, "def");
		Model model2 = ModelFactory.createDefaultModel();

		// direction 1
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertEquals(0, processor.getCorrespondenceGroups(aspect(1)).count());

		// direction 2
		processor = new JaroWinklerMappingProcessor().addInputPrimaryModel(dataset(1), model2)
				.addInputPrimaryModel(dataset(2), model1).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertEquals(0, processor.getCorrespondenceGroups(aspect(1)).count());
	}

	@Test
	public void handleZeroMappings() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDFS.label, "def");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource("entity2"), RDFS.label, "abc");
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertEquals(0, processor.getCorrespondenceGroups(aspect(1)).count());
	}

	@Test
	public void commutativ() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDFS.label, "aaaaaaaaaaa")
				.add(resource("entity2"), RDFS.label, "aaaaaaaaaab");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource("entity3"), RDFS.label, "aaaaaaaaaaa")
				.add(resource("entity4"), RDFS.label, "ccccccccccc");

		// direction 1
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertTrue(processor.allCorrespondend(resource("entity1"), resource("entity3")));
		assertEquals(1, processor.getCorrespondenceGroups(aspect(1)).count());

		// direction 2
		processor = new JaroWinklerMappingProcessor().addInputPrimaryModel(dataset(1), model2)
				.addInputPrimaryModel(dataset(2), model1).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertTrue(processor.allCorrespondend(resource("entity1"), resource("entity3")));
		assertEquals(1, processor.getCorrespondenceGroups(aspect(1)).count());

	}

	@Test
	public void caseSensitivity() throws Exception {
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource("entity1"), RDFS.label, "abc");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource("entity2"), RDFS.label, "ABC");

		// case-insensitive
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = false;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertTrue(processor.allCorrespondend(resource("entity1"), resource("entity2")));
		assertEquals(1, processor.getCorrespondenceGroups(aspect(1)).count());

		// case-sensitive
		processor = new JaroWinklerMappingProcessor().addInputPrimaryModel(dataset(1), model1)
				.addInputPrimaryModel(dataset(2), model2).addAspects(aspect1);
		processor.caseSensitive = true;
		processor.threshold = 0.90D;
		processor.aspect = aspect(1);
		processor.variables = Collections.singleton("label");
		processor.run();
		assertEquals(0, processor.getCorrespondenceGroups(aspect(1)).count());
	}
}
