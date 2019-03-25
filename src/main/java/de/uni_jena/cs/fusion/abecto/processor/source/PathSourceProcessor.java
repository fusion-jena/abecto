package de.uni_jena.cs.fusion.abecto.processor.source;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.util.ModelLoader;

public class PathSourceProcessor extends AbstractSourceProcessor {

	@Override
	public Model computeResultModel() throws Exception {
		String path = this.getProperty("path", new TypeLiteral<String>() {});
		return ModelLoader.getModel(new FileInputStream(path));
	}

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Collections.singletonMap("path", new TypeLiteral<String>() {});
	}

}
