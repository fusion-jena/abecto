package de.uni_jena.cs.fusion.abecto.processor;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.UploadSourceProcessor;
import de.uni_jena.cs.fusion.abecto.util.ModelUtils;

public class RdfFileSourceProcessor extends AbstractSourceProcessor<EmptyParameters>
		implements UploadSourceProcessor<EmptyParameters> {
	
	InputStream stream;

	@Override
	public Model computeResultModel() throws Exception {
		return ModelUtils.load(this.stream);
	}

	@Override
	public void setUploadStream(InputStream stream) {
		this.stream = stream;
	}

}
