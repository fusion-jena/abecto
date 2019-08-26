package de.uni_jena.cs.fusion.abecto.processor;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.ModelUtils;
import de.uni_jena.cs.fusion.abecto.processor.implementation.SparqlConstructProcessor;

public class SparqlConstructProcessorTest {
	@Test
	public void testComputeResultModel() throws Exception {
		String inputRdf = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
		Model inputModel = ModelUtils.load(new ByteArrayInputStream(inputRdf.getBytes()));
		SparqlConstructProcessor processor = new SparqlConstructProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		SparqlConstructProcessor.Parameter parameter = new SparqlConstructProcessor.Parameter();
		parameter.query = "CONSTRUCT {?s <http://example.org/x> <http://example.org/y>} WHERE {?s ?p ?o.}";
		processor.setParameters(parameter);
		Model outputModel = processor.call();
		outputModel.contains(ResourceFactory.createResource("http://example.org/s"),
				ResourceFactory.createProperty("http://example.org/x"),
				ResourceFactory.createResource("http://example.org/y"));
	}

}
