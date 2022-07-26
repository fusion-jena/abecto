package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

public class ForwardRuleReasoningProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// input model
		Model inputPrimaryModel = ModelFactory.createDefaultModel();
		inputPrimaryModel.add(r(1), p(1), r(2));
		inputPrimaryModel.add(r(2), p(1), r(3));
		inputPrimaryModel.add(r(3), p(1), r(4));
		inputPrimaryModel.add(r(4), p(1), r(5));
		inputPrimaryModel.add(r(5), p(1), r(6));

		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");

		// test maxIterations = 1
		ForwardRuleReasoningProcessor processor = new ForwardRuleReasoningProcessor()//
				.addInputPrimaryModel(dataset, inputPrimaryModel)//
				.setAssociatedDataset(dataset);
		processor.rules = "[rule1: (?s <" + p(1) + "> ?t) (?t <" + p(1) + "> ?o) -> (?s <" + p(1) + "> ?o)]";
		processor.run();
		Model outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(3)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(1), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(4)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(2), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(5)));
		assertTrue(outputPrimaryModel.contains(r(3), p(1), r(6)));

		assertTrue(outputPrimaryModel.contains(r(4), p(1), r(6)));

	}

	private Resource r(int i) {
		return ResourceFactory.createResource("http://example.org/r" + i);
	}

	private Property p(int i) {
		return ResourceFactory.createProperty("http://example.org/p" + i);
	}

}
