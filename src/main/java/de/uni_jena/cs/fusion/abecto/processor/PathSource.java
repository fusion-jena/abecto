package de.uni_jena.cs.fusion.abecto.processor;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.rdfhdt.hdt.exceptions.ParserException;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class PathSource extends AbstractProcessor implements SourceProcessor {

	@Override
	public RdfGraph call() {
		String path = this.getProperty("path", String.class);
		RdfGraph graph;
		try {
			graph = new RdfGraph(path);
			return graph;
		} catch (IOException | ParserException e) {
			this.listener.onFailure(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Class<?>> getPropertyTypes() {
		return Collections.singletonMap("path", String.class);
	}

}
