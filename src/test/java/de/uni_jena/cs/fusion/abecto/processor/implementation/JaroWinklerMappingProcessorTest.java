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
		FIRST_GRAPH = Models.load(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		SECOND_GRAPH = Models.load(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.com/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		META_GRAPH = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity",
				"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .", UUID.randomUUID()), META_GRAPH);
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

}
