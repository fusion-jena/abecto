package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;

import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

public class PathSourceProcessor extends AbstractSourceProcessor {

	@Override
	public RdfGraph computeResultGraph() throws Exception {
		String path = this.getProperty("path", new TypeLiteral<String>(){});
		RdfGraph graph = new RdfGraph(new FileInputStream(path));
		return graph;
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("path", new TypeLiteral<String>(){});
	}

}
