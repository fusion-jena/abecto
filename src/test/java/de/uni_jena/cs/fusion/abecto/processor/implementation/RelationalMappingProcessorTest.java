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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.RelationalMappingProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Metadata;
import de.uni_jena.cs.fusion.abecto.util.Models;

class RelationalMappingProcessorTest {

	@Test
	public void testComputeMapping() throws Exception {
		// preparation
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/1/> .\n"//
				+ ":entity1a :label \"entity1a\"     .\n"//
				+ ":entity1a :ref2a :entity2a        .\n"//
				+ ":entity1b :label \"entity1b\"     .\n"//
				+ ":entity1b :ref2b :entity2a        .\n"//
				+ ":entity3a :ref3a :entity1a        .\n"//
				+ ":entity3a :ref3b \"someLiteral\"  .\n"//
		).getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/2/> .\n"//
				+ ":entity1a :label \"entity1a\"     .\n"//
				+ ":entity1b :label \"entity1b\"     .\n"//
				+ ":entity2a :ref2a :entity1a        .\n"//
				+ ":entity2a :ref2b :entity1b        .\n"//
				+ ":entity3a :ref3a :entity1a        .\n"//
				+ ":entity3a :ref3b :entity1b        .\n"//
		).getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		SparqlEntityManager.insert(Arrays.asList(//
				new Category("dummy", "{"//
						+ "?dummy <http://example.org/1/label> ?label ."//
						+ "}"//
						, id1),
				new Category("valid", "{"//
						+ "?valid ^<http://example.org/1/ref2a> ?ref2a ."//
						+ "?valid ^<http://example.org/1/ref2b> ?ref2b ."//
						+ "}"//
						, id1),
				new Category("invalid", "{"//
						+ "?invalid <http://example.org/1/ref3a> ?ref3a ."//
						+ "?invalid <http://example.org/1/ref3b> ?ref3b ."//
						+ "}"//
						, id1),
				new Category("dummy", "{"//
						+ "?dummy <http://example.org/2/label> ?label ."//
						+ "}"//
						, id2),
				new Category("valid", "{"//
						+ "?valid <http://example.org/2/ref2a> ?ref2a ."//
						+ "?valid <http://example.org/2/ref2b> ?ref2b ."//
						+ "}"//
						, id2),
				new Category("invalid", "{"//
						+ "?invalid <http://example.org/2/ref3a> ?ref3a ."//
						+ "?invalid <http://example.org/2/ref3b> ?ref3b ."//
						+ "}"//
						, id2)),
				metaModel);
		SparqlEntityManager.insert(Arrays.asList(//
				Mapping.of(ResourceFactory.createResource("http://example.org/1/entity1a"),
						ResourceFactory.createResource("http://example.org/2/entity1a")),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/entity1b"),
						ResourceFactory.createResource("http://example.org/2/entity1b"))),
				metaModel);

		// result test
		RelationalMappingProcessor processor = new RelationalMappingProcessor();
		RelationalMappingProcessor.Parameter parameter = new RelationalMappingProcessor.Parameter();
		parameter.category = "valid";
		parameter.variables = Arrays.asList("ref2a", "ref2b");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		Set<Mapping> mappings = Metadata.getPositiveMappings(processor.getResultModel());
		assertEquals(1, mappings.size());
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/1/entity2a"),
				ResourceFactory.createResource("http://example.org/2/entity2a"))));

		assertTrue(SparqlEntityManager.select(new Issue(), processor.getResultModel()).isEmpty());

		// exception test
		processor = new RelationalMappingProcessor();
		parameter = new RelationalMappingProcessor.Parameter();
		parameter.category = "invalid";
		parameter.variables = Arrays.asList("ref3a", "ref3b");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		assertTrue(Metadata.getPositiveMappings(processor.getResultModel()).isEmpty());

		Issue issue = SparqlEntityManager.selectOne(new Issue(), processor.getResultModel()).orElseThrow();
		assertEquals(ResourceFactory.createResource("http://example.org/1/entity3a"), issue.entity);
		assertEquals(id1, issue.ontology);
		assertEquals("UnexpectedValueType", issue.type);
		assertEquals("Value of property \"ref3b\" is not a resource.", issue.message);
	}
}
