package de.uni_jena.cs.fusion.abecto.processor;

import java.io.FileInputStream;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.util.ModelUtils;

public class PathSourceProcessor extends AbstractSourceProcessor<PathSourceProcessorParameter> {

	@Override
	public Model computeResultModel() throws Exception {
		String path = this.getParameters().path;
		return ModelUtils.load(new FileInputStream(path));
	}

}
