package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping.AbstractMappingProcessor.Mapping;
import de.uni_jena.cs.fusion.abecto.util.GraphFactory;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class JaroWinklerMappingProcessorTest {

	private static Graph FIRST_GRAPH;
	private static Graph SECOND_GRAPH;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		FIRST_GRAPH = GraphFactory.getGraph(JaroWinklerMappingProcessorTest.class
				.getResourceAsStream("JaroWinklerMappingProcessorTest-example1.ttl"));
		SECOND_GRAPH = GraphFactory.getGraph(JaroWinklerMappingProcessorTest.class
				.getResourceAsStream("JaroWinklerMappingProcessorTest-example2.ttl"));
	}

	@Test
	public void testComputeMapping() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		processor.setProperties(Map.of("threshold", 0.90D, "case_sensitive", false));
		Collection<Mapping> mappings = processor.computeMapping(FIRST_GRAPH, SECOND_GRAPH);
		assertEquals(2, mappings.size());
		assertTrue(mappings.contains(Mapping.of((Node_URI) NodeFactory.createURI("http://example.org/entity1"),
				(Node_URI) NodeFactory.createURI("http://example.org/entity1"))));
		assertTrue(mappings.contains(Mapping.of((Node_URI) NodeFactory.createURI("http://example.org/entity2"),
				(Node_URI) NodeFactory.createURI("http://example.org/entity2"))));
	}

	@Test
	public void testComputeResultGraph() throws Exception {
		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		processor.setProperties(Map.of("threshold", 0.90D, "case_sensitive", false));
		processor.addInputGraphGroup(FIRST_GRAPH);
		processor.addInputGraphGroup(SECOND_GRAPH);
		Graph result = processor.computeResultGraph();
		List<Triple> triples = new ArrayList<>();
		result.find(Node.ANY, Node.ANY, Node.ANY).forEachRemaining(triples::add);
		assertEquals(2, triples.size());
		assertTrue(triples.contains(new Triple(NodeFactory.createURI("http://example.org/entity1"),
				Vocabulary.MAPPING_PROPERTY, NodeFactory.createURI("http://example.org/entity1"))));
		assertTrue(triples.contains(new Triple(NodeFactory.createURI("http://example.org/entity2"),
				Vocabulary.MAPPING_PROPERTY, NodeFactory.createURI("http://example.org/entity2"))));
	}

}
