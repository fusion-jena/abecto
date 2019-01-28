package de.uni_jena.cs.fusion.abecto.processor;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class StreamSourceProcessor extends AbstractSourceProcessor {

	@Override
	public RdfGraph computeResultGraph() throws Exception {
		InputStream stream = this.getProperty("stream", InputStream.class);
		RdfGraph graph = new RdfGraph(stream);
		return graph;
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("stream", InputStream.class);
	}

}
