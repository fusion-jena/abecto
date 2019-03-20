package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.util.GraphFactory;

public class PathSourceProcessor extends AbstractSourceProcessor {

	@Override
	public Graph computeResultGraph() throws Exception {
		String path = this.getProperty("path", new TypeLiteral<String>() {
		});
		return GraphFactory.getGraph(new FileInputStream(path));
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("path", new TypeLiteral<String>() {
		});
	}

}
