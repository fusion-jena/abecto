package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.util.ModelUtils;

public class StreamSourceProcessor extends AbstractSourceProcessor {

	@Override
	public Model computeResultModel() throws Exception {
		InputStream stream = this.getParameter("stream", new TypeLiteral<InputStream>() {});
		return ModelUtils.load(stream);
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("stream", new TypeLiteral<InputStream>() {});
	}

}
