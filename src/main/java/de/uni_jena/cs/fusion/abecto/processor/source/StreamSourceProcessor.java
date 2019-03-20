package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.graph.Graph;

import de.uni_jena.cs.fusion.abecto.util.GraphFactory;

public class StreamSourceProcessor extends AbstractSourceProcessor {

	@Override
	public Graph computeResultGraph() throws Exception {
		InputStream stream = this.getProperty("stream", new TypeLiteral<InputStream>() {
		});
		return GraphFactory.getGraph(stream);
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("stream", new TypeLiteral<InputStream>() {
		});
	}

}
