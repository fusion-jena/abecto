package de.uni_jena.cs.fusion.abecto.processor.api;

import java.io.IOException;
import java.io.InputStream;

public interface UploadSourceProcessor<P extends ParameterModel> extends SourceProcessor<P> {

	public void setUploadStream(InputStream stream) throws IOException;

}
