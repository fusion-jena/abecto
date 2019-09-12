package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.UploadSourceProcessor;

public class RdfFileSourceProcessor extends AbstractSourceProcessor<EmptyParameters>
		implements UploadSourceProcessor<EmptyParameters> {
	
	InputStream stream;

	@Override
	public Model computeResultModel() throws Exception {
		return Models.load(this.stream);
	}

	@Override
	public void setUploadStream(InputStream stream) {
		this.stream = stream;
	}

}
