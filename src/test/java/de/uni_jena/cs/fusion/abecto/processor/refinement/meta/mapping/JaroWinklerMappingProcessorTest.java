package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping.AbstractMappingProcessor.Mapping;
import de.uni_jena.cs.fusion.abecto.util.ModelLoader;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class JaroWinklerMappingProcessorTest {

	private static Model FIRST_GRAPH;
	private static Model SECOND_GRAPH;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		FIRST_GRAPH = ModelLoader.getModel(JaroWinklerMappingProcessorTest.class
				.getResourceAsStream("JaroWinklerMappingProcessorTest-example1.ttl"));
		SECOND_GRAPH = ModelLoader.getModel(JaroWinklerMappingProcessorTest.class
				.getResourceAsStream("JaroWinklerMappingProcessorTest-example2.ttl"));
	}

	@Test
	public void testComputeMapping() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		processor.setProperties(Map.of("threshold", 0.90D, "case_sensitive", false));
		Collection<Mapping> mappings = processor.computeMapping(FIRST_GRAPH, SECOND_GRAPH);
		assertEquals(2, mappings.size());
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.org/entity1"))));
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.org/entity2"))));
	}

	@Test
	public void testComputeResultGraph() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		processor.setProperties(Map.of("threshold", 0.90D, "case_sensitive", false));
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
