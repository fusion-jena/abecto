/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.processor;

import static de.uni_jena.cs.fusion.abecto.TestUtil.aspect;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsDeviation;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsIssue;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsMeasurement;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsValuesOmission;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.getMeasurement;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static de.uni_jena.cs.fusion.abecto.TestUtil.subject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import com.github.jsonldjava.shaded.com.google.common.base.Objects;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

public abstract class AbstractValueComparisonProcessorTest {

	private static class TestValueComparisonProcessor
			extends AbstractValueComparisonProcessor<TestValueComparisonProcessor> {

		public static TestValueComparisonProcessor getInstance() {
			TestValueComparisonProcessor processor = new TestValueComparisonProcessor();
			processor.variables = Collections.singletonList("value");
			processor.aspect = aspect(1);
			return processor;
		}

		@Override
		public boolean equivalentValues(RDFNode value1, RDFNode value2) {
			return Objects.equal(value1, value2);
		}

		@Override
		public String invalidValueComment() {
			return "";
		}

		@Override
		public boolean isValidValue(RDFNode value) {
			return true;
		}
	}

	Query pattern = QueryFactory.create("SELECT ?key ?value ?dummy WHERE { ?key <" + property(2)
			+ "> ?dummy OPTIONAL{?key <" + property(1) + "> ?value}}");
	Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
	Model mappingModel = ModelFactory.createDefaultModel();

	{
		addMapping(subject(1), subject(2), subject(3), subject(4));
	}

	public void addMapping(Resource... resources) {
		mappingModel.add(aspect(1), AV.relevantResource, resources[0]);
		for (Resource resource : Arrays.copyOfRange(resources, 1, resources.length)) {
			mappingModel.add(aspect(1), AV.relevantResource, resource);
			mappingModel.add(resources[0], AV.correspondsToResource, resource);
		}
	}

	void assertDeviation(Aspect aspect, Collection<RDFNode> values1, Collection<RDFNode> values2,
			Collection<RDFNode> notDeviatingValues1, Collection<RDFNode> notDeviatingValues2, int overlap)
			throws Exception {
		assertDeviationOneDirection(aspect, values1, values2, notDeviatingValues1, notDeviatingValues2, overlap);
		assertDeviationOneDirection(aspect, values2, values1, notDeviatingValues2, notDeviatingValues1, overlap);
	}

	void assertDeviation(Aspect aspect, RDFNode value1, RDFNode value2) throws Exception {
		assertDeviation(aspect, Collections.singleton(value1), Collections.singleton(value2), Collections.emptyList(),
				Collections.emptyList(), 0);
	}

	public void assertDeviation(Resource affectedDataset, Resource affectedResource, String affectedValue,
			Resource comparedToDataset, Resource comparedToResource, String comparedToValue, Model outputMetaModel,
			boolean expected) {
		boolean actual = containsDeviation(affectedResource, "value",
				ResourceFactory.createStringLiteral(affectedValue), comparedToDataset, comparedToResource,
				ResourceFactory.createStringLiteral(comparedToValue), aspect(1), outputMetaModel);
		if (expected) {
			assertTrue(actual, String.format(
					"Value \"%s\" of %s from %s and value \"%s\" of %s from %s should be reported as deviation.",
					affectedValue, affectedResource, affectedDataset, comparedToValue, comparedToResource,
					comparedToDataset));
		} else {
			assertFalse(actual, String.format(
					"Value \"%s\" of %s from %s and value \"%s\" of %s from %s should not be reported as deviation.",
					affectedValue, affectedResource, affectedDataset, comparedToValue, comparedToResource,
					comparedToDataset));
		}
	}

	void assertDeviationOneDirection(Aspect aspect, Collection<RDFNode> values1, Collection<RDFNode> values2,
			Collection<RDFNode> notDeviatingValues1, Collection<RDFNode> notDeviatingValues2, int overlap)
			throws Exception {
		int expectedDeviationCount = (values1.size() - notDeviatingValues1.size())
				* (values2.size() - notDeviatingValues2.size());
		// first direction
		Model model1 = ModelFactory.createDefaultModel();
		Model model2 = ModelFactory.createDefaultModel();
		for (RDFNode value1 : values1) {
			model1.add(subject(1), property(1), value1);
		}
		for (RDFNode value2 : values2) {
			model2.add(subject(2), property(1), value2);
		}
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(aspect, model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		for (RDFNode value1 : values1) {
			if (!notDeviatingValues1.contains(value1)) {
				for (RDFNode value2 : values2) {
					if (!notDeviatingValues2.contains(value2)) {
						assertTrue(containsDeviation(subject(1), "value", value1, dataset(2), subject(2), value2,
								aspect(1), outputMetaModels[0]));
						assertTrue(containsDeviation(subject(2), "value", value2, dataset(1), subject(1), value1,
								aspect(1), outputMetaModels[1]));
					}
				}
			}
		}
		assertEquals(expectedDeviationCount,
				outputMetaModels[0].listStatements(null, RDF.type, AV.Deviation).toList().size());
		assertEquals(expectedDeviationCount,
				outputMetaModels[1].listStatements(null, RDF.type, AV.Deviation).toList().size());

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(values1.size());
		BigDecimal expectedCount2 = new BigDecimal(values2.size());
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(values1.size());
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(values2.size());
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(notDeviatingValues1.size());
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(notDeviatingValues2.size());
		BigDecimal expectedRelativeCoverage1 = (expectedCount2.equals(BigDecimal.ZERO)) ? null
				: expectedAbsoluteCoverage1.divide(expectedCount2, AbstractValueComparisonProcessor.SCALE,
						RoundingMode.HALF_UP);
		BigDecimal expectedRelativeCoverage2 = (expectedCount1.equals(BigDecimal.ZERO)) ? null
				: expectedAbsoluteCoverage2.divide(expectedCount1, AbstractValueComparisonProcessor.SCALE,
						RoundingMode.HALF_UP);
		BigDecimal overlapD = new BigDecimal(overlap);
		BigDecimal expectedCompleteness1 = (overlapD.equals(BigDecimal.ZERO)) ? null
				: expectedCount1.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
						AbstractValueComparisonProcessor.SCALE, RoundingMode.HALF_UP));
		BigDecimal expectedCompleteness2 = (overlapD.equals(BigDecimal.ZERO)) ? null
				: expectedCount2.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
						AbstractValueComparisonProcessor.SCALE, RoundingMode.HALF_UP));
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2, 
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModels[0], outputMetaModels[1]);
	}

	private void assertMeasurements(BigDecimal expectedCount1, BigDecimal expectedCount2,
									BigDecimal expectedDeduplicatedCount1, BigDecimal expectedDeduplicatedCount2,
			BigDecimal expectedAbsoluteCoverage1, BigDecimal expectedAbsoluteCoverage2,
			@Nullable BigDecimal expectedRelativeCoverage1, @Nullable BigDecimal expectedRelativeCoverage2,
			@Nullable BigDecimal expectedCompleteness1, @Nullable BigDecimal expectedCompleteness2, Resource dataset1,
			Resource dataset2, Resource aspect1, Model outputMetaModel1, Model outputMetaModel2) {

		if (expectedCount1 != null) {
			assertEquals(expectedCount1,
					getMeasurement(AV.count, OM.one, dataset(1), "value", null, aspect(1), outputMetaModel1),
					"Wrong count for dataset 1.");
		}
		if (expectedCount2 != null) {
			assertEquals(expectedCount2,
					getMeasurement(AV.count, OM.one, dataset(2), "value", null, aspect(1), outputMetaModel2),
					"Wrong count for dataset 2.");
		}
		if (expectedDeduplicatedCount1 != null) {
			assertEquals(expectedDeduplicatedCount1,
					getMeasurement(AV.deduplicatedCount, OM.one, dataset(1), "value", null, aspect(1), outputMetaModel1),
					"Wrong deduplicated count for dataset 1.");
		}
		if (expectedDeduplicatedCount2 != null) {
			assertEquals(expectedDeduplicatedCount2,
					getMeasurement(AV.deduplicatedCount, OM.one, dataset(2), "value", null, aspect(1), outputMetaModel2),
					"Wrong deduplicated count for dataset 2.");
		}
		if (expectedAbsoluteCoverage1 != null) {
			assertEquals(
					expectedAbsoluteCoverage1, getMeasurement(AV.absoluteCoverage, OM.one, dataset(1), "value",
							Collections.singleton(dataset(2)), aspect(1), outputMetaModel1),
					"Wrong absolute coverage for dataset 1.");
		}
		if (expectedAbsoluteCoverage2 != null) {
			assertEquals(
					expectedAbsoluteCoverage2, getMeasurement(AV.absoluteCoverage, OM.one, dataset(2), "value",
							Collections.singleton(dataset(1)), aspect(1), outputMetaModel2),
					"Wrong absolute coverage for dataset 2.");
		}
		if (expectedRelativeCoverage1 != null) {
			assertEquals(
					expectedRelativeCoverage1.stripTrailingZeros(), getMeasurement(AV.relativeCoverage, OM.one,
							dataset(1), "value", Collections.singleton(dataset(2)), aspect(1), outputMetaModel1),
					"Wrong relative coverage for dataset 1.");
		} else {
			assertFalse(
					containsMeasurement(AV.relativeCoverage, null, OM.one, dataset(1), "value",
							Collections.singleton(dataset(2)), aspect(1), outputMetaModel1),
					"Unexpected existence of relative coverage for dataset 1.");
		}
		if (expectedRelativeCoverage2 != null) {
			assertEquals(
					expectedRelativeCoverage2.stripTrailingZeros(), getMeasurement(AV.relativeCoverage, OM.one,
							dataset(2), "value", Collections.singleton(dataset(1)), aspect(1), outputMetaModel2),
					"Wrong relative coverage of dataset 2.");
		} else {
			assertFalse(
					containsMeasurement(AV.relativeCoverage, null, OM.one, dataset(2), "value",
							Collections.singleton(dataset(1)), aspect(1), outputMetaModel2),
					"Unexpected existence of relative coverage for dataset 2.");
		}
		if (expectedCompleteness1 != null) {
			assertEquals(expectedCompleteness1.stripTrailingZeros(),
					getMeasurement(AV.marCompletenessThomas08, OM.one, dataset(1), "value",
							Collections.singleton(dataset(2)), aspect(1), outputMetaModel1),
					"Wrong completeness of dataset 1.");
		} else {
			assertFalse(
					containsMeasurement(AV.marCompletenessThomas08, null, OM.one, dataset(1), "value",
							Collections.singleton(dataset(2)), aspect(1), outputMetaModel1),
					"Unexpected existence of completeness for dataset 1.");
		}
		if (expectedCompleteness2 != null) {
			assertEquals(expectedCompleteness2.stripTrailingZeros(),
					getMeasurement(AV.marCompletenessThomas08, OM.one, dataset(2), "value",
							Collections.singleton(dataset(1)), aspect(1), outputMetaModel2),
					"Wrong completeness of dataset 2.");
		} else {
			assertFalse(
					containsMeasurement(AV.marCompletenessThomas08, null, OM.one, dataset(2), "value",
							Collections.singleton(dataset(1)), aspect(1), outputMetaModel2),
					"Unexpected existence of completeness for dataset 2.");
		}
	}

	void assertMissing(Aspect aspect, Collection<RDFNode> values1, Collection<RDFNode> values2,
			Collection<RDFNode> missingValues1, Collection<RDFNode> missingValues2, int overlap) throws Exception {
		assertMissingOneDirection(aspect, values1, values2, missingValues1, missingValues2, overlap);
		assertMissingOneDirection(aspect, values2, values1, missingValues2, missingValues1, overlap);
	}

	void assertMissingOneDirection(Aspect aspect, Collection<RDFNode> values1, Collection<RDFNode> values2,
			Collection<RDFNode> missingValues1, Collection<RDFNode> missingValues2, int overlap) throws Exception {
		Model model1 = ModelFactory.createDefaultModel();
		Model model2 = ModelFactory.createDefaultModel();
		for (RDFNode value1 : values1) {
			model1.add(subject(1), property(1), value1);
		}
		for (RDFNode value2 : values2) {
			model2.add(subject(2), property(1), value2);
		}
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(aspect, model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertEquals(missingValues1.size(),
				outputMetaModels[0].listStatements(null, RDF.type, AV.ValueOmission).toList().size());
		assertEquals(missingValues2.size(),
				outputMetaModels[1].listStatements(null, RDF.type, AV.ValueOmission).toList().size());
		for (RDFNode missingValue1 : missingValues1) {
			assertTrue(containsValuesOmission(subject(1), "value", dataset(2), subject(2), missingValue1, aspect(1),
					outputMetaModels[0]));
		}
		for (RDFNode missingValue2 : missingValues2) {
			assertTrue(containsValuesOmission(subject(2), "value", dataset(1), subject(1), missingValue2, aspect(1),
					outputMetaModels[1]));
		}

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(values1.size());
		BigDecimal expectedCount2 = new BigDecimal(values2.size());
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(values1.size());
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(values2.size());
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(values2.size() - missingValues1.size());
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(values1.size() - missingValues2.size());
		BigDecimal expectedRelativeCoverage1 = (expectedCount2.equals(BigDecimal.ZERO)) ? null
				: expectedAbsoluteCoverage1.divide(expectedCount2, AbstractValueComparisonProcessor.SCALE,
						RoundingMode.HALF_UP);
		BigDecimal expectedRelativeCoverage2 = (expectedCount1.equals(BigDecimal.ZERO)) ? null
				: expectedAbsoluteCoverage2.divide(expectedCount1, AbstractValueComparisonProcessor.SCALE,
						RoundingMode.HALF_UP);
		BigDecimal overlapD = new BigDecimal(overlap);
		BigDecimal expectedCompleteness1 = (overlapD.equals(BigDecimal.ZERO)) ? null
				: expectedCount1.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
						AbstractValueComparisonProcessor.SCALE, RoundingMode.HALF_UP));
		BigDecimal expectedCompleteness2 = (overlapD.equals(BigDecimal.ZERO)) ? null
				: expectedCount2.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
						AbstractValueComparisonProcessor.SCALE, RoundingMode.HALF_UP));
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2, 
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModels[0], outputMetaModels[1]);
	}

	public void assertMissingValue(Resource affectedDataset, Resource affectedResource, Resource comparedToDataset,
			Resource comparedToResource, String missingValue, Model outputMetaModel, boolean expected) {
		boolean actual = containsValuesOmission(affectedResource, "value", comparedToDataset, comparedToResource,
				ResourceFactory.createStringLiteral(missingValue), aspect(1), outputMetaModel);
		if (expected) {
			assertTrue(actual,
					String.format("Value \"%s\" for %s from %s should be reported as missing compared to %s from %s.",
							missingValue, affectedResource, affectedDataset, comparedToResource, comparedToDataset));
		} else {
			assertFalse(actual,
					String.format(
							"Value \"%s\" for %s from %s should not be reported as missing compared to %s from %s.",
							missingValue, affectedResource, affectedDataset, comparedToResource, comparedToDataset));
		}
	}

	void assertSame(Aspect aspect, RDFNode value1, RDFNode value2) throws Exception {
		assertSameOneDirection(aspect, value2, value1);
		assertSameOneDirection(aspect, value1, value2);
	}

	void assertSameOneDirection(Aspect aspect, RDFNode value1, RDFNode value2) throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), value1);
		Model model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), value2);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(aspect, model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));
		// check measurement

		// assert measurements
		BigDecimal expectedCount1 = BigDecimal.ONE;
		BigDecimal expectedCount2 = BigDecimal.ONE;
		BigDecimal expectedDeduplicatedCount1 = BigDecimal.ONE;
		BigDecimal expectedDeduplicatedCount2 = BigDecimal.ONE;
		BigDecimal expectedAbsoluteCoverage1 = BigDecimal.ONE;
		BigDecimal expectedAbsoluteCoverage2 = BigDecimal.ONE;
		BigDecimal expectedRelativeCoverage1 = BigDecimal.ONE;
		BigDecimal expectedRelativeCoverage2 = BigDecimal.ONE;
		BigDecimal expectedCompleteness1 = BigDecimal.ONE;
		BigDecimal expectedCompleteness2 = BigDecimal.ONE;
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModels[0], outputMetaModels[1]);
	}

	void assertUnexpectedValueType(Aspect aspect, RDFNode expectedValue, RDFNode unexpectedValue, String issueComment)
			throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), unexpectedValue);
		Model model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), expectedValue);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(aspect, model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertTrue(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		assertTrue(containsIssue(subject(1), "value", unexpectedValue, aspect(1), "Invalid Value", issueComment,
				outputMetaModels[0]));
		assertEquals(1, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(0, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());

		// assert measurements first direction
		BigDecimal expectedCount1 = BigDecimal.ONE;
		BigDecimal expectedCount2 = BigDecimal.ONE;
		BigDecimal expectedDeduplicatedCount1 = BigDecimal.ONE;
		BigDecimal expectedDeduplicatedCount2 = BigDecimal.ONE;
		BigDecimal expectedAbsoluteCoverage1 = BigDecimal.ZERO;
		BigDecimal expectedAbsoluteCoverage2 = BigDecimal.ZERO;
		BigDecimal expectedRelativeCoverage1 = BigDecimal.ZERO;
		BigDecimal expectedRelativeCoverage2 = BigDecimal.ZERO;
		BigDecimal expectedCompleteness1 = null;
		BigDecimal expectedCompleteness2 = null;
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModels[0], outputMetaModels[1]);

		// second direction
		model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), expectedValue);
		model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), unexpectedValue);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		outputMetaModels = compare(aspect, model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertTrue(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		assertTrue(containsIssue(subject(2), "value", unexpectedValue, aspect(1), "Invalid Value", issueComment,
				outputMetaModels[1]));
		assertEquals(0, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(1, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());

		// assert measurements second direction
		// some expected values as for first direction
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModels[0], outputMetaModels[1]);
	}

	Model[] compare(Aspect aspect, Model model1, Model model2) throws Exception {
		return compare(aspect, getInstance(Collections.singletonList("value"), aspect(1)), model1, model2);
	}

	Model[] compare(Aspect aspect, Processor<?> processor, Model model1, Model model2) throws Exception {
		processor.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2)
				.addInputMetaModel(null, MappingProcessor.inferTransitiveCorrespondences(mappingModel))
				.addAspects(aspect);
		processor.run();
		return new Model[] { processor.getOutputMetaModel(dataset(1)), processor.getOutputMetaModel(dataset(2)) };
	}

	@Test
	public void duplicatesWithAllValuesComparedToDuplicatesWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1", "value2"),
				Arrays.asList("value1", "value2"), true, Arrays.asList("value1", "value2"),
				Arrays.asList("value1", "value2"), true);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(4);
		BigDecimal expectedCount2 = new BigDecimal(4);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(2);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(2);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(1);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void duplicatesWithAllValuesComparedToSingleWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList("value1"),
				true, Arrays.asList("value1"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(2);
		BigDecimal expectedCount2 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(1);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void duplicatesWithComplementaryValuesComparedToDuplicatesWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList("value2"),
				true, Arrays.asList("value1", "value2"), Arrays.asList("value1", "value2"), true);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, true);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, true);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, true);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(2);
		BigDecimal expectedCount2 = new BigDecimal(4);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(2);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(2);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(1);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void duplicatesWithComplementaryValuesComparedToSingleWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList("value2"),
				true, Arrays.asList("value1", "value2"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, true);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, true);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(2);
		BigDecimal expectedCount2 = new BigDecimal(2);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(2);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(2);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(1);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void duplicatesWithMissingValuesComparedToDuplicatesWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList("value1"),
				true, Arrays.asList("value1", "value2"), Arrays.asList("value1", "value2"), true);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, true);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, true);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(2);
		BigDecimal expectedCount2 = new BigDecimal(4);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(0.5);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(0.5);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void duplicatesWithMissingValuesComparedToSingleWithAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList(), true,
				Arrays.asList("value1", "value2"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(1);
		BigDecimal expectedCount2 = new BigDecimal(2);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(2);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(0.5);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(0.5);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	public abstract Processor<?> getInstance(List<String> variables, Resource aspect);

	private Model[] prepareAndRunComparison(Aspect aspect, Collection<String> valuesR1D1, Collection<String> valuesR2D1,
			boolean presentR2D1, Collection<String> valuesR1D2, Collection<String> valuesR2D2, boolean presentR2D2)
			throws Exception {
		Model model1 = ModelFactory.createDefaultModel();
		valuesR1D1.forEach(v -> model1.add(subject(1), property(1), v));
		valuesR2D1.forEach(v -> model1.add(subject(2), property(1), v));
		Model model2 = ModelFactory.createDefaultModel();
		valuesR1D2.forEach(v -> model2.add(subject(3), property(1), v));
		valuesR2D2.forEach(v -> model2.add(subject(4), property(1), v));

		model1.add(subject(1), property(2), resource("alwaysPresent"));
		if (presentR2D1)
			model1.add(subject(2), property(2), resource("alwaysPresent"));
		model2.add(subject(3), property(2), resource("alwaysPresent"));
		if (presentR2D2)
			model2.add(subject(4), property(2), resource("alwaysPresent"));

		TestValueComparisonProcessor processor = TestValueComparisonProcessor.getInstance();

		return compare(aspect, processor, model1, model2);
	}

	@Test
	public void singleToSingleAllValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList(), false,
				Arrays.asList("value1"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(1);
		BigDecimal expectedCount2 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(1);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(1);
		BigDecimal expectedCompleteness1 = new BigDecimal(1);
		BigDecimal expectedCompleteness2 = new BigDecimal(1);
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void singleToSingleDifferentValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1"), Arrays.asList(), false,
				Arrays.asList("value2"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, true);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, true);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(1);
		BigDecimal expectedCount2 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(0);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(0);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(0);
		BigDecimal expectedRelativeCoverage2 = new BigDecimal(0);
		BigDecimal expectedCompleteness1 = null;
		BigDecimal expectedCompleteness2 = null;
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void singleToSingleMissingValues() throws Exception {
		Model[] outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList(), Arrays.asList(), false,
				Arrays.asList("value1"), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, true);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(0);
		BigDecimal expectedCount2 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(0);
		BigDecimal expectedDeduplicatedCount2 = new BigDecimal(1);
		BigDecimal expectedAbsoluteCoverage1 = new BigDecimal(0);
		BigDecimal expectedAbsoluteCoverage2 = new BigDecimal(0);
		BigDecimal expectedRelativeCoverage1 = new BigDecimal(0);
		BigDecimal expectedRelativeCoverage2 = null;
		BigDecimal expectedCompleteness1 = null;
		BigDecimal expectedCompleteness2 = null;
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}

	@Test
	public void countsAtSingleDatasetWithVariable() throws Exception {
		// check counts if one variable is covered by only one dataset (dataset 1)
		Query patternWithoutValue = QueryFactory
				.create("SELECT ?key ?dummy WHERE { ?key <" + property(2) + "> ?dummy}");
		Aspect aspectWithIncompleteVarCoverage = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern)
				.setPattern(dataset(2), patternWithoutValue);
		Model[] outputMetaModels = prepareAndRunComparison(aspectWithIncompleteVarCoverage, Arrays.asList("value1"),
				Arrays.asList(), false, Arrays.asList(), Arrays.asList(), false);
		Model outputMetaModel1 = outputMetaModels[0];
		Model outputMetaModel2 = outputMetaModels[1];

		// omissions subject 1
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(1), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 2
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value1", outputMetaModel1, false);
		assertMissingValue(dataset(1), subject(2), dataset(2), subject(4), "value2", outputMetaModel1, false);

		// omissions subject 3
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(3), dataset(2), subject(4), "value2", outputMetaModel2, false);

		// omissions subject 4
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value1", outputMetaModel2, false);
		assertMissingValue(dataset(2), subject(4), dataset(2), subject(3), "value2", outputMetaModel2, false);

		// deviations subject 1
		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 2
		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModel1, false);

		assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModel1, false);
		assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModel1, false);

		// deviations subject 3
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModel2, false);
		// deviations subject 4
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModel2, false);

		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModel2, false);
		assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModel2, false);

		// assert measurements
		BigDecimal expectedCount1 = new BigDecimal(1);
		BigDecimal expectedCount2 = null;
		BigDecimal expectedDeduplicatedCount1 = new BigDecimal(1);
		BigDecimal expectedDeduplicatedCount2 = null;
		BigDecimal expectedAbsoluteCoverage1 = null;
		BigDecimal expectedAbsoluteCoverage2 = null;
		BigDecimal expectedRelativeCoverage1 = null;
		BigDecimal expectedRelativeCoverage2 = null;
		BigDecimal expectedCompleteness1 = null;
		BigDecimal expectedCompleteness2 = null;
		assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
				expectedAbsoluteCoverage1, expectedAbsoluteCoverage2,
				expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2,
				dataset(1), dataset(2), aspect(1), outputMetaModel1, outputMetaModel2);
	}
}
