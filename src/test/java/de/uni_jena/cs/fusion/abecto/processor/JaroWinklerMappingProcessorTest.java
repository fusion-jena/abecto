package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractMappingProcessor.Mapping;
import de.uni_jena.cs.fusion.abecto.util.ModelUtils;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class JaroWinklerMappingProcessorTest {

	private static Model FIRST_GRAPH;
	private static Model SECOND_GRAPH;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		FIRST_GRAPH = ModelUtils.load(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		SECOND_GRAPH = ModelUtils.load(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
	}

	@Test
	public void testComputeMapping() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessorParameter parameter = new JaroWinklerMappingProcessorParameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		processor.setParameters(parameter);
		Collection<Mapping> mappings = processor.computeMapping(FIRST_GRAPH, SECOND_GRAPH);
		assertEquals(2, mappings.size());
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.org/entity1"))));
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.org/entity2"))));
	}

	@Test
	public void testComputeResultModel() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessorParameter parameter = new JaroWinklerMappingProcessorParameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(FIRST_GRAPH), UUID.randomUUID(),
				Collections.singleton(SECOND_GRAPH)));
		Model result = processor.computeResultModel();
		List<Statement> statements = new ArrayList<>();
		result.listStatements().forEachRemaining(statements::add);
		assertEquals(2, statements.size());
		assertTrue(statements
				.contains(ResourceFactory.createStatement(ResourceFactory.createResource("http://example.org/entity1"),
						Vocabulary.MAPPING, ResourceFactory.createResource("http://example.org/entity1"))));
		assertTrue(statements
				.contains(ResourceFactory.createStatement(ResourceFactory.createResource("http://example.org/entity2"),
						Vocabulary.MAPPING, ResourceFactory.createResource("http://example.org/entity2"))));
	}

}
