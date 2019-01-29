package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class PathSourceProcessor extends AbstractSourceProcessor {

	@Override
	public RdfGraph computeResultGraph() throws Exception {
		String path = this.getProperty("path", String.class);
		RdfGraph graph = new RdfGraph(new FileInputStream(path));
		return graph;
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("path", String.class);
	}

}
