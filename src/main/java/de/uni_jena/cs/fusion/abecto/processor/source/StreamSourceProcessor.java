package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class StreamSourceProcessor extends AbstractSourceProcessor {

	@Override
	public RdfGraph computeResultGraph() throws Exception {
		InputStream stream = this.getProperty("stream", new TypeLiteral<InputStream>() {
		});
		RdfGraph graph = new RdfGraph(stream);
		return graph;
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("stream", new TypeLiteral<InputStream>() {
		});
	}

}
