package de.uni_jena.cs.fusion.abecto.processor;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.util.ModelUtils;

public class StreamSourceProcessor extends AbstractSourceProcessor<StreamSourceProcessorParameter> {

	@Override
	public Model computeResultModel() throws Exception {
		InputStream stream = this.getParameters().stream;
		return ModelUtils.load(stream);
	}

	@Override
	public Class<StreamSourceProcessorParameter> getParameterModel() {
		return StreamSourceProcessorParameter.class;
	}

}
