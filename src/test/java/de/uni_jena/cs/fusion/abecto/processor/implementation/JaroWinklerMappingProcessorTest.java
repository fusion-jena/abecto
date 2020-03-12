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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class JaroWinklerMappingProcessorTest {

	private static Model FIRST_GRAPH;
	private static Model SECOND_GRAPH;
	private static Model META_GRAPH;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		FIRST_GRAPH = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		SECOND_GRAPH = Models.read(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.com/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		META_GRAPH = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity",
				"{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}", UUID.randomUUID()), META_GRAPH);
	}

	@Test
	public void testComputeMapping() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addMetaModels(Collections.singleton(META_GRAPH));
		Collection<Mapping> mappings = processor.computeMapping(FIRST_GRAPH, SECOND_GRAPH, UUID.randomUUID(),
				UUID.randomUUID());
		assertEquals(2, mappings.size());
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.com/entity1"))));
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.com/entity2"))));
	}

	@Test
	public void testComputeResultModel() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(FIRST_GRAPH), UUID.randomUUID(),
				Collections.singleton(SECOND_GRAPH)));
		processor.addMetaModels(Collections.singleton(META_GRAPH));
		processor.computeResultModel();
		Model result = processor.getResultModel();
		Collection<Mapping> positiveMappings = SparqlEntityManager.select(Mapping.of(), result);
		Collection<Mapping> negativeMappings = SparqlEntityManager.select(Mapping.not(), result);
		assertEquals(2, positiveMappings.size());
		assertTrue(positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.com/entity1"))));
		assertTrue(positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.com/entity2"))));
		assertTrue(negativeMappings.isEmpty());
	}

	@Test
	public void handelOptionalValue() throws Exception {
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
				, UUID.randomUUID()), metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(model1), UUID.randomUUID(),
				Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
		Model result = processor.getResultModel();
		Collection<Mapping> positiveMappings = SparqlEntityManager.select(Mapping.of(), result);
		Collection<Mapping> negativeMappings = SparqlEntityManager.select(Mapping.not(), result);
		assertTrue(positiveMappings.isEmpty());
		assertTrue(negativeMappings.isEmpty());
	}

	@Test
	public void handelEmptyModels() throws Exception {
		Model model = Models.read(new ByteArrayInputStream((""//
				+ "@base <http://example.org/> .\n"//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"abc\"  .\n"//
				+ ":entity2 :label \"def\" .").getBytes()));
		Model modelEmpty = Models.getEmptyOntModel();
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://example.org/label> ?label .}", UUID.randomUUID()), metaModel);
		JaroWinklerMappingProcessor processor;
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");

		// direction 1
		processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(modelEmpty), UUID.randomUUID(),
				Collections.singleton(model)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();

		// direction 2
		processor = new JaroWinklerMappingProcessor();
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(model), UUID.randomUUID(),
				Collections.singleton(modelEmpty)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
	}

	@Test
	public void handleZeroMappings() throws Exception {
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity1 :label \"abc\" .").getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix : <http://example.org/> .\n"//
				+ ":entity2 :label \"def\" .").getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(
				new Category("entity", "{?entity <http://example.org/label> ?label .}", UUID.randomUUID()), metaModel);
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(model1), UUID.randomUUID(),
				Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.computeResultModel();
	}

	@Test
	public void commutativ() throws Exception {
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
				, UUID.randomUUID()), metaModel);

		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");

		UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

		// direction 1
		{
			JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
			processor.setParameters(parameter);
			processor.addInputModelGroups(
					Map.of(uuid1, Collections.singleton(model1), uuid2, Collections.singleton(model2)));
			processor.addMetaModels(Collections.singleton(metaModel));
			processor.computeResultModel();
			Model result = processor.getResultModel();

			Collection<Mapping> positiveMappings = SparqlEntityManager.select(Mapping.of(), result);
			assertEquals(1, positiveMappings.size());
			assertTrue(
					positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
							ResourceFactory.createResource("http://example.org/entity3"))));
			assertTrue(SparqlEntityManager.select(Mapping.not(), result).isEmpty());
		}
		// direction 2
		{
			JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
			processor.setParameters(parameter);
			processor.addInputModelGroups(
					Map.of(uuid1, Collections.singleton(model2), uuid2, Collections.singleton(model1)));
			processor.addMetaModels(Collections.singleton(metaModel));
			processor.computeResultModel();
			Model result = processor.getResultModel();

			Collection<Mapping> positiveMappings = SparqlEntityManager.select(Mapping.of(), result);
			assertEquals(1, positiveMappings.size());
			assertTrue(
					positiveMappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
							ResourceFactory.createResource("http://example.org/entity3"))));
			assertTrue(SparqlEntityManager.select(Mapping.not(), result).isEmpty());
		}

	}

}
