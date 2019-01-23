package de.uni_jena.cs.fusion.abecto.processor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class PathSource extends AbstractProcessor implements SourceProcessor {

	@Override
	public RdfGraph call() {
		try {
			String path = this.getProperty("path", String.class);
			RdfGraph graph = new RdfGraph(new FileInputStream(path));
			return graph;
		} catch (IOException e) {
			this.listener.onFailure(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("path", String.class);
	}

}
