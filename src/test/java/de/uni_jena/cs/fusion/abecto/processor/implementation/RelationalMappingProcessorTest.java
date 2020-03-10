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

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class RelationalMappingProcessorTest {

	@Test
	public void testComputeMapping() throws Exception {
		// preparation
		Model model1 = Models.load(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/1/> .\n"//
				+ ":entity1a :label \"entity1a\"     .\n"//
				+ ":entity1a :ref2a :entity2a        .\n"//
				+ ":entity1b :label \"entity1b\"     .\n"//
				+ ":entity1b :ref2b :entity2a        .\n"//
				+ ":entity3a :ref3a :entity1a        .\n"//
				+ ":entity3a :ref3b \"someLiteral\"  .\n"//
		).getBytes()));
		Model model2 = Models.load(new ByteArrayInputStream((""//
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

		Set<Mapping> mappings = SparqlEntityManager.select(Mapping.of(), processor.getResultModel());
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

		assertTrue(SparqlEntityManager.select(Mapping.of(), processor.getResultModel()).isEmpty());

		Issue issue = SparqlEntityManager.selectOne(new Issue(), processor.getResultModel()).orElseThrow();
		assertEquals(ResourceFactory.createResource("http://example.org/1/entity3a"), issue.entity);
		assertEquals(id1, issue.knowledgeBase);
		assertEquals("UnexpectedValueType", issue.type);
		assertEquals("Value of property \"ref3b\" is not a resource.", issue.message);
	}
}
