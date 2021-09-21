package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class StepTest {
	@Test
	public void constructor()
			throws IllegalArgumentException, IOException, ClassCastException, ReflectiveOperationException {
		Model defaultModel = ModelFactory.createDefaultModel();

		String stepIri = "http://example.org/step";
		String stepIriNotJavaScheme = "http://example.org/stepNotJavaScheme";
		String stepIriNotProcessorSubclass = "http://example.org/stepNotProcessorSubclass";
		Integer expectedParameterValue = 123;
		Resource aspectResource = ResourceFactory.createResource("http://example.org/aspect");

		Map<Resource, Aspect> aspectsMap = Map.of(aspectResource, new Aspect(aspectResource, "key"));

		Resource parameter = defaultModel.createResource(AV.Parameter).addLiteral(AV.key, "integerParam")
				.addLiteral(RDF.value, expectedParameterValue);

		// provide valid step definition
		defaultModel.createResource(stepIri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.hasParameter, parameter);
		// provide invalid step definition
		defaultModel.createResource(stepIriNotJavaScheme, AV.Step)
				.addProperty(AV.processorClass, ResourceFactory.createResource("http://example.org/javaClass"))
				.addProperty(AV.hasParameter, stepIriNotProcessorSubclass);
		// provide invalid step definition
		defaultModel.createResource(stepIriNotProcessorSubclass, AV.Step)
				.addProperty(AV.processorClass, ResourceFactory.createResource("java:" + Model.class.getName()))
				.addProperty(AV.hasParameter, parameter);

		Dataset dataset = DatasetFactory.createGeneral();
		dataset.setDefaultModel(defaultModel);
		@SuppressWarnings("unused")
		Step step = new Step(dataset, dataset.getDefaultModel(), ResourceFactory.createResource(stepIri),
				Collections.emptyList(), aspectsMap);

		assertThrows(IllegalArgumentException.class, () -> new Step(dataset, dataset.getDefaultModel(),
				ResourceFactory.createResource(stepIriNotJavaScheme), Collections.emptyList(), aspectsMap));
		assertThrows(IllegalArgumentException.class, () -> new Step(dataset, dataset.getDefaultModel(),
				ResourceFactory.createResource(stepIriNotProcessorSubclass), Collections.emptyList(), aspectsMap));

		// check processor parameter set
		assertEquals(expectedParameterValue, TestProcessor.instance.integerParam);

		// check processor aspects set
		assertEquals(aspectsMap, TestProcessor.instance.getAspects());
	}

	public static class TestProcessor extends Processor {
		static TestProcessor instance;

		@Parameter
		public Integer integerParam;

		public TestProcessor() {
			instance = this;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}

	}

}
