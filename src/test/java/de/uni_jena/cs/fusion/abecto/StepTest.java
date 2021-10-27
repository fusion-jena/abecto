/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;
import de.uni_jena.cs.fusion.abecto.vocabulary.PROV;

public class StepTest {
	public static final Property META_IN_EMPTY = ResourceFactory.createProperty("http://example.org/metaInEmpty");
	public static final Property PRIMARY_IN_EMPTY = ResourceFactory.createProperty("http://example.org/primaryInEmpty");
	public static final Resource ALL_DATASETS = ResourceFactory.createResource("http://example.org/allDataset");
	public static final Property STEP_NUMBER = ResourceFactory.createProperty("http://example.org/stepNumber");
	private static File relativeBasePath;

	@BeforeAll
	public static void setRelativeBasePath() throws IOException {
		relativeBasePath = Files.createTempDirectory(null).toFile();
	}

	@Test
	public void constructor()
			throws IllegalArgumentException, IOException, ClassCastException, ReflectiveOperationException {
		Model defaultModel = ModelFactory.createDefaultModel();

		String stepIri = "http://example.org/step";
		String stepIriNotJavaScheme = "http://example.org/stepNotJavaScheme";
		String stepIriNotProcessorSubclass = "http://example.org/stepNotProcessorSubclass";
		Integer expectedParameterValue = 123;
		Resource aspectResource = ResourceFactory.createResource("http://example.org/aspect");
		Aspect aspect = new Aspect(aspectResource, "key");

		Resource parameter = defaultModel.createResource(AV.Parameter).addLiteral(AV.key, "integerParameter")
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
		Step step = new Step(relativeBasePath, dataset, dataset.getDefaultModel(),
				ResourceFactory.createResource(stepIri), Collections.emptyList(), aspect);

		assertThrows(IllegalArgumentException.class,
				() -> new Step(relativeBasePath, dataset, dataset.getDefaultModel(),
						ResourceFactory.createResource(stepIriNotJavaScheme), Collections.emptyList(), aspect));
		assertThrows(IllegalArgumentException.class,
				() -> new Step(relativeBasePath, dataset, dataset.getDefaultModel(),
						ResourceFactory.createResource(stepIriNotProcessorSubclass), Collections.emptyList(), aspect));

		// check processor parameter set
		assertEquals(expectedParameterValue, TestProcessor.instance.integerParameter);

		// check processor aspects set
		assertEquals(aspect, TestProcessor.instance.getAspects().get(aspectResource));
	}

	@Test
	public void run() throws IllegalArgumentException, ClassCastException, ReflectiveOperationException {
		Abecto.initApacheJena();

		Resource dataset1 = ResourceFactory.createResource("http://example.org/dataset1");
		Resource dataset2 = ResourceFactory.createResource("http://example.org/dataset2");
		String step1Iri = "http://example.org/step1-1";
		String step2Iri = "http://example.org/step1-2";
		String step3Iri = "http://example.org/step2-1";
		String step4Iri = "http://example.org/step2-2";
		String step5Iri = "http://example.org/stepAll";

		Resource aspectResource = ResourceFactory.createResource("http://example.org/aspect");
		Aspect aspect = new Aspect(aspectResource, "key");
		Model configurationModel = ModelFactory.createDefaultModel();
		Dataset graphs = DatasetFactory.createGeneral();
		graphs.setDefaultModel(configurationModel);

		Model inputMetaDataModel = ModelFactory.createDefaultModel();
		inputMetaDataModel.addLiteral(dataset2, STEP_NUMBER, 0);
		Resource inputMetaDataModelIri = ResourceFactory.createResource("http://example.org/inputMetaDataModel");
		graphs.addNamedModel(inputMetaDataModelIri.getURI(), inputMetaDataModel);

		configurationModel.createResource(step1Iri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.associatedDataset, dataset1).addProperty(AV.hasParameter, configurationModel
						.createResource(AV.Parameter).addLiteral(AV.key, "integerParameter").addLiteral(RDF.value, 1));

		configurationModel.createResource(step2Iri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.associatedDataset, dataset1).addProperty(AV.hasParameter, configurationModel
						.createResource(AV.Parameter).addLiteral(AV.key, "integerParameter").addLiteral(RDF.value, 2));

		configurationModel.createResource(step3Iri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.associatedDataset, dataset2)
				.addProperty(AV.hasParameter, configurationModel.createResource(AV.Parameter)
						.addLiteral(AV.key, "integerParameter").addLiteral(RDF.value, 3))
				.addProperty(AV.inputMetaDataGraph, inputMetaDataModelIri);

		configurationModel.createResource(step4Iri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.associatedDataset, dataset2).addProperty(AV.hasParameter, configurationModel
						.createResource(AV.Parameter).addLiteral(AV.key, "integerParameter").addLiteral(RDF.value, 4));

		configurationModel.createResource(step5Iri, AV.Step)
				.addProperty(AV.processorClass,
						ResourceFactory.createResource("java:" + StepTest.TestProcessor.class.getName()))
				.addProperty(AV.hasParameter, configurationModel.createResource(AV.Parameter)
						.addLiteral(AV.key, "integerParameter").addLiteral(RDF.value, 5));

		Step step1 = new Step(relativeBasePath, graphs, graphs.getDefaultModel(),
				configurationModel.createResource(step1Iri), Collections.emptyList(), aspect);
		step1.run();
		Step step2 = new Step(relativeBasePath, graphs, graphs.getDefaultModel(),
				configurationModel.createResource(step2Iri), Collections.singletonList(step1), aspect);
		step2.run();
		Step step3 = new Step(relativeBasePath, graphs, graphs.getDefaultModel(),
				configurationModel.createResource(step3Iri), Collections.emptyList(), aspect);
		step3.run();
		Step step4 = new Step(relativeBasePath, graphs, graphs.getDefaultModel(),
				configurationModel.createResource(step4Iri), Collections.singletonList(step3), aspect);
		step4.run();
		Step step5 = new Step(relativeBasePath, graphs, graphs.getDefaultModel(),
				configurationModel.createResource(step5Iri), Arrays.asList(step1, step2, step3, step4), aspect);
		step5.run();

		Resource step1Execution = configurationModel
				.listSubjectsWithProperty(PPlan.correspondsToStep, configurationModel.createResource(step1Iri)).next();
		Resource step2Execution = configurationModel
				.listSubjectsWithProperty(PPlan.correspondsToStep, configurationModel.createResource(step2Iri)).next();
		Resource step3Execution = configurationModel
				.listSubjectsWithProperty(PPlan.correspondsToStep, configurationModel.createResource(step3Iri)).next();
		Resource step4Execution = configurationModel
				.listSubjectsWithProperty(PPlan.correspondsToStep, configurationModel.createResource(step4Iri)).next();
		Resource step5Execution = configurationModel
				.listSubjectsWithProperty(PPlan.correspondsToStep, configurationModel.createResource(step5Iri)).next();

		assertEquals(step1.getStepExecution(), step1Execution);
		assertEquals(step2.getStepExecution(), step2Execution);
		assertEquals(step3.getStepExecution(), step3Execution);
		assertEquals(step4.getStepExecution(), step4Execution);
		assertEquals(step5.getStepExecution(), step5Execution);

		assertValidExecutionTime(step1Execution);
		assertValidExecutionTime(step2Execution);
		assertValidExecutionTime(step3Execution);
		assertValidExecutionTime(step4Execution);
		assertValidExecutionTime(step5Execution);

		assertModelContains(graphs, step1Execution, AV.PrimaryDataGraph, dataset1, STEP_NUMBER, 1);
		assertModelContains(graphs, step2Execution, AV.PrimaryDataGraph, dataset1, STEP_NUMBER, 2);
		assertModelContains(graphs, step3Execution, AV.PrimaryDataGraph, dataset2, STEP_NUMBER, 3);
		assertModelContains(graphs, step4Execution, AV.PrimaryDataGraph, dataset2, STEP_NUMBER, 4);

		assertModelContains(graphs, step1Execution, AV.MetaDataGraph, dataset1, STEP_NUMBER, 1);
		assertModelContains(graphs, step2Execution, AV.MetaDataGraph, dataset1, STEP_NUMBER, 2);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset1, STEP_NUMBER, 5);

		assertModelContains(graphs, step3Execution, AV.MetaDataGraph, dataset2, STEP_NUMBER, 3);
		assertModelContains(graphs, step4Execution, AV.MetaDataGraph, dataset2, STEP_NUMBER, 4);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset2, STEP_NUMBER, 5);

		assertModelContains(graphs, step1Execution, AV.MetaDataGraph, null, STEP_NUMBER, 1);
		assertModelContains(graphs, step2Execution, AV.MetaDataGraph, null, STEP_NUMBER, 2);
		assertModelContains(graphs, step3Execution, AV.MetaDataGraph, null, STEP_NUMBER, 3);
		assertModelContains(graphs, step4Execution, AV.MetaDataGraph, null, STEP_NUMBER, 4);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, null, STEP_NUMBER, 5);

		assertModelContains(graphs, step1Execution, AV.MetaDataGraph, dataset1, META_IN_EMPTY, 1);
		assertModelContains(graphs, step2Execution, AV.MetaDataGraph, dataset1, META_IN_EMPTY, 0);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset1, META_IN_EMPTY, 0);

		assertModelContains(graphs, step3Execution, AV.MetaDataGraph, dataset2, META_IN_EMPTY, 1);
		assertModelContains(graphs, step4Execution, AV.MetaDataGraph, dataset2, META_IN_EMPTY, 0);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset2, META_IN_EMPTY, 0);

		assertModelContains(graphs, step1Execution, AV.MetaDataGraph, null, META_IN_EMPTY, 1);
		assertModelContains(graphs, step2Execution, AV.MetaDataGraph, null, META_IN_EMPTY, 0);
		// due to inputMetaDataGraph not empty for step3Execution
		assertModelContains(graphs, step3Execution, AV.MetaDataGraph, null, META_IN_EMPTY, 0);
		assertModelContains(graphs, step4Execution, AV.MetaDataGraph, null, META_IN_EMPTY, 0);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, null, META_IN_EMPTY, 0);

		assertModelContains(graphs, step1Execution, AV.MetaDataGraph, dataset1, PRIMARY_IN_EMPTY, 1);
		assertModelContains(graphs, step2Execution, AV.MetaDataGraph, dataset1, PRIMARY_IN_EMPTY, 0);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset1, PRIMARY_IN_EMPTY, 0);

		assertModelContains(graphs, step3Execution, AV.MetaDataGraph, dataset2, PRIMARY_IN_EMPTY, 1);
		assertModelContains(graphs, step4Execution, AV.MetaDataGraph, dataset2, PRIMARY_IN_EMPTY, 0);
		assertModelContains(graphs, step5Execution, AV.MetaDataGraph, dataset2, PRIMARY_IN_EMPTY, 0);
	}

	private void assertValidExecutionTime(Resource execution) {
		assertTrue(((OffsetDateTime) execution.listProperties(PROV.startedAtTime).next().getLiteral().getValue())
				.compareTo(
						(OffsetDateTime) execution.listProperties(PROV.endedAtTime).next().getLiteral().getValue()) < 0,
				String.format("Invalid execution times found at execution of %s.",
						execution.getPropertyResourceValue(PPlan.correspondsToStep).getURI()));
	}

	private void assertModelContains(Dataset graphs, Resource execution, Resource outputModelType, Resource dataset,
			Property property, int... expectedValues) throws AssertionFailedError {
		Model configurationModel = graphs.getDefaultModel();
		ExtendedIterator<Resource> iterator = configurationModel
				.listResourcesWithProperty(PROV.wasGeneratedBy, execution)
				.filterKeep(r -> r.hasProperty(RDF.type, outputModelType));
		if (dataset == null) {
			iterator = iterator.filterDrop(r -> r
					.hasProperty(AV.MetaDataGraph.equals(outputModelType) ? DQV.computedOn : AV.associatedDataset));
		} else {
			iterator = iterator.filterKeep(r -> r.hasProperty(
					AV.MetaDataGraph.equals(outputModelType) ? DQV.computedOn : AV.associatedDataset, dataset));
		}
		Model outputMetaModel = graphs.getNamedModel(iterator.next().getURI());

		int[] actualValues = outputMetaModel
				.listObjectsOfProperty(Objects.requireNonNullElse(dataset, ALL_DATASETS), property).toList().stream()
				.mapToInt(node -> node.asLiteral().getInt()).sorted().toArray();
		Arrays.sort(expectedValues);
		assertArrayEquals(expectedValues, actualValues, () -> String.format("Expected: %s but was: %s",
				Arrays.toString(expectedValues), Arrays.toString(actualValues)));
	}

	public static class TestProcessor extends Processor<TestProcessor> {
		static TestProcessor instance;

		@Parameter
		public int integerParameter;

		public TestProcessor() {
			instance = this;
		}

		@Override
		public void run() {
			if (this.getAssociatedDataset().isPresent()) {
				Resource dataset = this.getAssociatedDataset().get();
				this.getOutputPrimaryModel().get().addLiteral(dataset, STEP_NUMBER, this.integerParameter);
				this.getOutputMetaModel(dataset).addLiteral(dataset, STEP_NUMBER, this.integerParameter);
			}

			for (Resource dataset : this.getDatasets()) {
				Model inMeta = this.getInputMetaModelUnion(dataset);
				Model inPrimary = this.getInputPrimaryModelUnion(dataset);
				Model out = this.getOutputMetaModel(dataset);
				out.addLiteral(dataset, META_IN_EMPTY, inMeta.listStatements().hasNext() ? 0 : 1);
				out.addLiteral(dataset, PRIMARY_IN_EMPTY, inPrimary.listStatements().hasNext() ? 0 : 1);
				out.addLiteral(dataset, STEP_NUMBER, this.integerParameter);
			}

			{
				Model inMeta = this.getInputMetaModelUnion(null);
				Model out = this.getOutputMetaModel(null);
				out.addLiteral(ALL_DATASETS, META_IN_EMPTY, inMeta.listStatements().hasNext() ? 0 : 1);
				out.addLiteral(ALL_DATASETS, STEP_NUMBER, this.integerParameter);
			}
		}

	}

}
