package de.uni_jena.cs.fusion.abecto.processor;

import java.io.IOException;
import java.io.InputStream;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public interface UploadSourceProcessor<P extends ParameterModel> extends SourceProcessor<P> {

	public void setUploadStream(InputStream stream) throws IOException;

}
