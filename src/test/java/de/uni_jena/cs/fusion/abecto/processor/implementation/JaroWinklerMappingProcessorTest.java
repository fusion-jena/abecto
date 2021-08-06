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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Mappings;
import de.uni_jena.cs.fusion.abecto.util.Models;

public class JaroWinklerMappingProcessorTest {
	@BeforeAll
	public static void initJena() {
		// ensure Jena initialization
		JenaSystem.init();
	}

	@Test
	public void testComputeMapping() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.com/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId2),
				metaModel);
		SparqlEntityManager.insert(
				new Category("other", "{?other <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(
				new Category("other", "{?other <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId2),
				metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addMetaModels(Collections.singleton(metaModel));
		Collection<Mapping> mappings = processor.computeMapping(model1, model2, ontologyId1, ontologyId2);
		assertEquals(2, mappings.size());
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.com/entity1"))));
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.com/entity2"))));
	}

	@Test
	public void testComputeResultModel() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.com/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", ontologyId2),
				metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
		Model result = processor.getResultModel();
		Collection<Mapping> positiveMappings = Mappings.getPositiveMappings(result);
		Collection<Mapping> negativeMappings = Mappings.getNegativeMappings(result);
		assertEquals(2, positiveMappings.size());
		assertTrue(positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.com/entity1"))));
		assertTrue(positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.com/entity2"))));
		assertTrue(negativeMappings.isEmpty());
	}

	@Test
	public void handelOptionalValue() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@base <http://example.org/> .\n"//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :type  :Thing  .\n"//
				+ ":entity2 :type  :Thing  ;\n"//
				+ "         :label \"def\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@base <http://example.org/> .\n"//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity3 :type  :Thing  ;\n"//
				+ "         :label \"abc\" .\n"//
				+ ":entity4 :type  :Thing  .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity", "{"//
				+ "?entity <http://example.org/type> <http://example.org/Thing> ."//
				+ "OPTIONAL {?entity <http://example.org/label> ?label}"//
				+ "}"//
				, ontologyId1), metaModel);
		SparqlEntityManager.insert(new Category("entity", "{"//
				+ "?entity <http://example.org/type> <http://example.org/Thing> ."//
				+ "OPTIONAL {?entity <http://example.org/label> ?label}"//
				+ "}"//
				, ontologyId2), metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
		Model result = processor.getResultModel();
		Collection<Mapping> positiveMappings = Mappings.getPositiveMappings(result);
		Collection<Mapping> negativeMappings = Mappings.getNegativeMappings(result);
		assertTrue(positiveMappings.isEmpty());
		assertTrue(negativeMappings.isEmpty());
	}

	@Test
	public void handelEmptyModels() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model = Models.read(new ByteArrayInputStream((""//
				+ "@base <http://example.org/> .\n"//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"abc\"  .\n"//
				+ ":entity2 :label \"def\" .").getBytes()));
		Model modelEmpty = Models.getEmptyOntModel();
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId2),
				metaModel);
		JaroWinklerMappingProcessor processor;
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");

		// direction 1
		processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(modelEmpty), ontologyId2, Collections.singleton(model)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();

		// direction 2
		processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		processor.addInputModelGroups(
				Map.of(ontologyId2, Collections.singleton(model), ontologyId1, Collections.singleton(modelEmpty)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
	}

	@Test
	public void handleZeroMappings() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"abc\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity2 :label \"def\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId2),
				metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
	}

	@Test
	public void commutativ() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"aaaaaaaaaaa\"  .\n"//
				+ ":entity2 :label \"aaaaaaaaaab\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity3 :label \"aaaaaaaaaaa\" .\n"//
				+ ":entity4 :label \"ccccccccccc\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity", "{"//
				+ "?entity <http://example.org/label> ?label"//
				+ "}"//
				, ontologyId1), metaModel);
		SparqlEntityManager.insert(new Category("entity", "{"//
				+ "?entity <http://example.org/label> ?label"//
				+ "}"//
				, ontologyId2), metaModel);

		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");

		// direction 1
		{
			JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
			processor.setParameters(parameter);
			processor.addInputModelGroups(
					Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
			processor.addMetaModels(Collections.singleton(metaModel));
			processor.computeResultModel();
			Model result = processor.getResultModel();

			Collection<Mapping> positiveMappings = Mappings.getPositiveMappings(result);
			assertEquals(1, positiveMappings.size());
			assertTrue(
					positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
							ResourceFactory.createResource("http://example.org/entity3"))));
			assertTrue(Mappings.getNegativeMappings(result).isEmpty());
		}
		// direction 2
		{
			JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
			processor.setParameters(parameter);
			processor.addInputModelGroups(
					Map.of(ontologyId1, Collections.singleton(model2), ontologyId2, Collections.singleton(model1)));
			processor.addMetaModels(Collections.singleton(metaModel));
			processor.computeResultModel();
			Model result = processor.getResultModel();

			Collection<Mapping> positiveMappings = Mappings.getPositiveMappings(result);
			assertEquals(1, positiveMappings.size());
			assertTrue(
					positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
							ResourceFactory.createResource("http://example.org/entity3"))));
			assertTrue(Mappings.getNegativeMappings(result).isEmpty());
		}
	}

	@Test
	public void caseSensitivity() throws Exception {
		UUID ontologyId1 = UUID.randomUUID();
		UUID ontologyId2 = UUID.randomUUID();
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"abc\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity2 :label \"ABC\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId1),
				metaModel);
		SparqlEntityManager.insert(new Category("entity", "{?entity <http://example.org/label> ?label .}", ontologyId2),
				metaModel);
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");

		// case-insensitive
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		parameter.case_sensitive = false;
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
		Model result = processor.getResultModel();

		Collection<Mapping> positiveMappings = Mappings.getPositiveMappings(result);
		assertEquals(1, positiveMappings.size());
		assertTrue(positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.org/entity2"))));

		// case-sensitive
		processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		parameter.case_sensitive = true;
		processor.addInputModelGroups(
				Map.of(ontologyId1, Collections.singleton(model1), ontologyId2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
		result = processor.getResultModel();

		positiveMappings = Mappings.getPositiveMappings(result);
		assertTrue(positiveMappings.isEmpty());
	}
}
