/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.processor;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static de.uni_jena.cs.fusion.abecto.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class PropertyComparisonProcessorTest {

    Query pattern = QueryFactory.create("SELECT ?key ?value ?dummy WHERE { ?key <" + property(2)
            + "> ?dummy OPTIONAL{?key <" + property(1) + "> ?value}}");
    Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
    Model mappingModel = ModelFactory.createDefaultModel();
    Model[] outputMetaModels;

    {
        addMapping(subject(1), subject(2), subject(3), subject(4));
    }

    @BeforeAll
    public static void initJena() {
        JenaSystem.init();
    }

    public void addMapping(Resource... resources) {
        for (Resource resource : Arrays.copyOfRange(resources, 1, resources.length)) {
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
        outputMetaModels = compare(aspect, model1, model2);
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
        BigDecimal expectedAbsoluteCoverage = new BigDecimal(notDeviatingValues1.size());
        BigDecimal expectedRelativeCoverage1 = (expectedCount2.equals(BigDecimal.ZERO)) ? null
                : expectedAbsoluteCoverage.divide(expectedCount2, PropertyComparisonProcessor.SCALE,
                RoundingMode.HALF_UP);
        BigDecimal expectedRelativeCoverage2 = (expectedCount1.equals(BigDecimal.ZERO)) ? null
                : expectedAbsoluteCoverage.divide(expectedCount1, PropertyComparisonProcessor.SCALE,
                RoundingMode.HALF_UP);
        BigDecimal overlapD = new BigDecimal(overlap);
        BigDecimal expectedCompleteness1 = (overlapD.equals(BigDecimal.ZERO)) ? null
                : expectedCount1.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
                PropertyComparisonProcessor.SCALE, RoundingMode.HALF_UP));
        BigDecimal expectedCompleteness2 = (overlapD.equals(BigDecimal.ZERO)) ? null
                : expectedCount2.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
                PropertyComparisonProcessor.SCALE, RoundingMode.HALF_UP));
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }


    private void assertMeasurements(BigDecimal expectedCount1, BigDecimal expectedCount2,
                                    BigDecimal expectedDeduplicatedCount1, BigDecimal expectedDeduplicatedCount2,
                                    BigDecimal expectedAbsoluteCoverage,
                                    @Nullable BigDecimal expectedRelativeCoverage1, @Nullable BigDecimal expectedRelativeCoverage2,
                                    @Nullable BigDecimal expectedCompleteness1, @Nullable BigDecimal expectedCompleteness2) {
        assertIndependentMeasurement(AV.count, 1, expectedCount1);
        assertIndependentMeasurement(AV.count, 2, expectedCount2);
        assertIndependentMeasurement(AV.deduplicatedCount, 1, expectedDeduplicatedCount1);
        assertIndependentMeasurement(AV.deduplicatedCount, 2, expectedDeduplicatedCount2);
        assertComparativeMeasurement(AV.absoluteCoverage, 1, expectedAbsoluteCoverage);
        assertComparativeMeasurement(AV.relativeCoverage, 1, expectedRelativeCoverage1);
        assertComparativeMeasurement(AV.relativeCoverage, 2, expectedRelativeCoverage2);
        assertComparativeMeasurement(AV.marCompletenessThomas08, 1, expectedCompleteness1);
        assertComparativeMeasurement(AV.marCompletenessThomas08, 2, expectedCompleteness2);
    }

    void assertIndependentMeasurement(Resource measure, int computedOnDatasetNumber, BigDecimal expectedValue) {
        if (expectedValue != null) {
            assertMeasurement(measure, computedOnDatasetNumber, null, expectedValue);
        } else {
            assertNoIndependentMeasurement(measure, computedOnDatasetNumber);
        }
    }

    void assertNoIndependentMeasurement(Resource measure, int computedOnDatasetNumber) {
        assertFalse(
                containsMeasurement(measure, null, null, dataset(computedOnDatasetNumber), "value",
                        Collections.emptySet(), aspect(1), outputMetaModels[computedOnDatasetNumber - 1]),
                "Unexpected existence of " + measure.getLocalName() + " for dataset " + computedOnDatasetNumber + ".");
    }

    void assertComparativeMeasurement(Resource measure, int computedOnDatasetNumber, BigDecimal expectedValue) {
        if (expectedValue != null) {
            Collection<Resource> comparedToDatasets = Collections.singleton(dataset(3 - computedOnDatasetNumber));
            assertMeasurement(measure, computedOnDatasetNumber, comparedToDatasets, expectedValue);
        } else {
            assertNoComparativeMeasurement(measure, computedOnDatasetNumber);
        }
    }

    void assertMeasurement(Resource measure, int computedOnDatasetNumber, Collection<Resource> comparedToDatasets, BigDecimal expectedValue) {
        assertEquals(expectedValue.stripTrailingZeros(),
                getMeasurement(measure, OM.one, dataset(computedOnDatasetNumber), "value",
                        comparedToDatasets, aspect(1), outputMetaModels[computedOnDatasetNumber - 1]).stripTrailingZeros(),
                "Wrong " + measure.getLocalName() + " value for dataset " + computedOnDatasetNumber + ".");
    }

    void assertNoComparativeMeasurement(Resource measure, int computedOnDatasetNumber) {
        Collection<Resource> comparedToDatasets = Collections.singleton(dataset(3 - computedOnDatasetNumber));
        assertFalse(
                containsMeasurement(measure, null, null, dataset(computedOnDatasetNumber), "value",
                        comparedToDatasets, aspect(1), outputMetaModels[computedOnDatasetNumber - 1]),
                "Unexpected existence of " + measure.getLocalName() + " for dataset " + computedOnDatasetNumber + ".");
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
        outputMetaModels = compare(aspect, model1, model2);
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
        BigDecimal expectedAbsoluteCoverage = new BigDecimal(values2.size() - missingValues1.size());
        BigDecimal expectedRelativeCoverage1 = (expectedCount2.equals(BigDecimal.ZERO)) ? null
                : expectedAbsoluteCoverage.divide(expectedCount2, PropertyComparisonProcessor.SCALE,
                RoundingMode.HALF_UP);
        BigDecimal expectedRelativeCoverage2 = (expectedCount1.equals(BigDecimal.ZERO)) ? null
                : expectedAbsoluteCoverage.divide(expectedCount1, PropertyComparisonProcessor.SCALE,
                RoundingMode.HALF_UP);
        BigDecimal overlapD = new BigDecimal(overlap);
        BigDecimal expectedCompleteness1 = (overlapD.equals(BigDecimal.ZERO)) ? null
                : expectedCount1.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
                PropertyComparisonProcessor.SCALE, RoundingMode.HALF_UP));
        BigDecimal expectedCompleteness2 = (overlapD.equals(BigDecimal.ZERO)) ? null
                : expectedCount2.divide(expectedCount1.multiply(expectedCount2).divide(overlapD,
                PropertyComparisonProcessor.SCALE, RoundingMode.HALF_UP));
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    public void assertValueMissing(int affectedDatasetNumber, Resource affectedResource, int comparedToDatasetNumber,
                                   Resource comparedToResource, String missingValue) {
        Resource affectedDataset = dataset(affectedDatasetNumber);
        Resource comparedToDataset = dataset(comparedToDatasetNumber);
        boolean actual = containsValuesOmission(affectedResource, "value", comparedToDataset, comparedToResource,
                ResourceFactory.createStringLiteral(missingValue), aspect(1), outputMetaModels[affectedDatasetNumber - 1]);
        assertTrue(actual,
                String.format("Value \"%s\" for %s from %s should be reported as missing compared to %s from %s.",
                        missingValue, affectedResource, affectedDataset, comparedToResource, comparedToDataset));
    }

    public void assertValueNotMissing(int affectedDatasetNumber, Resource affectedResource, int comparedToDatasetNumber,
                                      Resource comparedToResource, String missingValue) {
        Resource affectedDataset = dataset(affectedDatasetNumber);
        Resource comparedToDataset = dataset(comparedToDatasetNumber);
        boolean actual = containsValuesOmission(affectedResource, "value", comparedToDataset, comparedToResource,
                ResourceFactory.createStringLiteral(missingValue), aspect(1), outputMetaModels[affectedDatasetNumber - 1]);
        assertFalse(actual,
                String.format(
                        "Value \"%s\" for %s from %s should not be reported as missing compared to %s from %s.",
                        missingValue, affectedResource, affectedDataset, comparedToResource, comparedToDataset));
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
        outputMetaModels = compare(aspect, model1, model2);
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
        BigDecimal expectedAbsoluteCoverage = BigDecimal.ONE;
        BigDecimal expectedRelativeCoverage1 = BigDecimal.ONE;
        BigDecimal expectedRelativeCoverage2 = BigDecimal.ONE;
        BigDecimal expectedCompleteness1 = BigDecimal.ONE;
        BigDecimal expectedCompleteness2 = BigDecimal.ONE;
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    Model[] compare(Aspect aspect, Model model1, Model model2) throws Exception {
        PropertyComparisonProcessor processor = new PropertyComparisonProcessor();
        processor.variables = Collections.singletonList("value");
        processor.aspect = aspect(1);
        return compare(aspect, processor, model1, model2);
    }

    Model[] compare(Aspect aspect, Processor<?> processor, Model model1, Model model2) {
        processor.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2)
                .addInputMetaModel(null, MappingProcessor.inferTransitiveCorrespondences(mappingModel))
                .addAspects(aspect);
        processor.run();
        return new Model[]{processor.getOutputMetaModel(dataset(1)), processor.getOutputMetaModel(dataset(2))};
    }

    @Test
    public void duplicatesWithAllValuesComparedToDuplicatesWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, Arrays.asList("value1", "value2"),
                Arrays.asList("value1", "value2"), true, Arrays.asList("value1", "value2"),
                Arrays.asList("value1", "value2"), true);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("4");
        BigDecimal expectedCount2 = new BigDecimal("4");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("2");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("2");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("2");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("1");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void duplicatesWithAllValuesComparedToSingleWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of("value1"),
                true, List.of("value1"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("2");
        BigDecimal expectedCount2 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("1");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("1");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void duplicatesWithComplementaryValuesComparedToDuplicatesWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of("value2"),
                true, Arrays.asList("value1", "value2"), Arrays.asList("value1", "value2"), true);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], true);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], true);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("2");
        BigDecimal expectedCount2 = new BigDecimal("4");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("2");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("2");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("2");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("1");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void duplicatesWithComplementaryValuesComparedToSingleWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of("value2"),
                true, Arrays.asList("value1", "value2"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], true);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], true);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("2");
        BigDecimal expectedCount2 = new BigDecimal("2");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("2");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("2");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("2");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("1");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void duplicatesWithMissingValuesComparedToDuplicatesWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of("value1"),
                true, Arrays.asList("value1", "value2"), Arrays.asList("value1", "value2"), true);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("2");
        BigDecimal expectedCount2 = new BigDecimal("4");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("2");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("0.5");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("0.5");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void duplicatesWithMissingValuesComparedToSingleWithAllValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of(), true,
                Arrays.asList("value1", "value2"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueMissing(1, subject(2), 2, subject(3), "value1");
        assertValueMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("1");
        BigDecimal expectedCount2 = new BigDecimal("2");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("2");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("0.5");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("0.5");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

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
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of(), false,
                List.of("value1"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("1");
        BigDecimal expectedCount2 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("1");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("1");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("1");
        BigDecimal expectedCompleteness1 = new BigDecimal("1");
        BigDecimal expectedCompleteness2 = new BigDecimal("1");
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void singleToSingleDifferentValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of("value1"), List.of(), false,
                List.of("value2"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], true);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], true);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("1");
        BigDecimal expectedCount2 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("1");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("0");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("0");
        BigDecimal expectedRelativeCoverage2 = new BigDecimal("0");
        BigDecimal expectedCompleteness1 = null;
        BigDecimal expectedCompleteness2 = null;
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void singleToSingleMissingValues() throws Exception {
        outputMetaModels = prepareAndRunComparison(aspect1, List.of(), List.of(), false,
                List.of("value1"), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("0");
        BigDecimal expectedCount2 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("0");
        BigDecimal expectedDeduplicatedCount2 = new BigDecimal("1");
        BigDecimal expectedAbsoluteCoverage = new BigDecimal("0");
        BigDecimal expectedRelativeCoverage1 = new BigDecimal("0");
        BigDecimal expectedRelativeCoverage2 = null;
        BigDecimal expectedCompleteness1 = null;
        BigDecimal expectedCompleteness2 = null;
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void countsAtSingleDatasetWithVariable() throws Exception {
        // check counts if one variable is covered by only one dataset (dataset 1)
        Query patternWithoutValue = QueryFactory
                .create("SELECT ?key ?dummy WHERE { ?key <" + property(2) + "> ?dummy}");
        Aspect aspectWithIncompleteVarCoverage = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern)
                .setPattern(dataset(2), patternWithoutValue);
        outputMetaModels = prepareAndRunComparison(aspectWithIncompleteVarCoverage, List.of("value1"),
                List.of(), false, List.of(), List.of(), false);

        // omissions subject 1
        assertValueNotMissing(1, subject(1), 1, subject(2), "value1");
        assertValueNotMissing(1, subject(1), 1, subject(2), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(1), 2, subject(4), "value2");

        // omissions subject 2
        assertValueNotMissing(1, subject(2), 1, subject(1), "value1");
        assertValueNotMissing(1, subject(2), 1, subject(1), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(3), "value2");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value1");
        assertValueNotMissing(1, subject(2), 2, subject(4), "value2");

        // omissions subject 3
        assertValueNotMissing(2, subject(3), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(3), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value1");
        assertValueNotMissing(2, subject(3), 2, subject(4), "value2");

        // omissions subject 4
        assertValueNotMissing(2, subject(4), 1, subject(1), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(1), "value2");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value1");
        assertValueNotMissing(2, subject(4), 1, subject(2), "value2");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value1");
        assertValueNotMissing(2, subject(4), 2, subject(3), "value2");

        // deviations subject 1
        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value1", dataset(1), subject(2), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(1), "value2", dataset(1), subject(2), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(1), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 2
        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value1", dataset(1), subject(1), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(3), "value2", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value1", dataset(2), subject(4), "value2", outputMetaModels[0], false);

        assertDeviation(dataset(1), subject(2), "value2", dataset(1), subject(1), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(3), "value1", outputMetaModels[0], false);
        assertDeviation(dataset(1), subject(2), "value2", dataset(2), subject(4), "value1", outputMetaModels[0], false);

        // deviations subject 3
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value1", dataset(2), subject(4), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(3), "value2", dataset(2), subject(4), "value1", outputMetaModels[1], false);
        // deviations subject 4
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(1), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(1), subject(2), "value2", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value1", dataset(2), subject(3), "value2", outputMetaModels[1], false);

        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(1), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(1), subject(2), "value1", outputMetaModels[1], false);
        assertDeviation(dataset(2), subject(4), "value2", dataset(2), subject(3), "value1", outputMetaModels[1], false);

        // assert measurements
        BigDecimal expectedCount1 = new BigDecimal("1");
        BigDecimal expectedCount2 = null;
        BigDecimal expectedDeduplicatedCount1 = new BigDecimal("1");
        BigDecimal expectedDeduplicatedCount2 = null;
        BigDecimal expectedAbsoluteCoverage = null;
        BigDecimal expectedRelativeCoverage1 = null;
        BigDecimal expectedRelativeCoverage2 = null;
        BigDecimal expectedCompleteness1 = null;
        BigDecimal expectedCompleteness2 = null;
        assertMeasurements(expectedCount1, expectedCount2, expectedDeduplicatedCount1, expectedDeduplicatedCount2,
                expectedAbsoluteCoverage,
                expectedRelativeCoverage1, expectedRelativeCoverage2, expectedCompleteness1, expectedCompleteness2);
    }

    @Test
    public void allowLangTagSkip() {
        PropertyComparisonProcessor processor = new PropertyComparisonProcessor();
        String lex = "lex";

        assertTrue(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createLangLiteral(lex, "")));
        assertFalse(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createLangLiteral(lex, "en")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createLangLiteral(lex, "")));
        assertFalse(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createLangLiteral(lex, "")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createLangLiteral(lex, "en")));

        processor.allowLangTagSkip = true;

        assertTrue(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createLangLiteral(lex, "")));
        assertTrue(processor.equivalentValues(ResourceFactory.createStringLiteral(lex),
                ResourceFactory.createLangLiteral(lex, "en")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createLangLiteral(lex, "")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, ""),
                ResourceFactory.createLangLiteral(lex, "en")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createLangLiteral(lex, "")));
        assertTrue(processor.equivalentValues(ResourceFactory.createLangLiteral(lex, "en"),
                ResourceFactory.createLangLiteral(lex, "en")));

    }

    @Test
    void isExcludedValue() {
        PropertyComparisonProcessor processor = new PropertyComparisonProcessor();
        String lex = "";

        // default
        assertFalse(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));

        // none
        processor.languageFilterPatterns = List.of("");
        assertFalse(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));

        // any
        processor.languageFilterPatterns = List.of("*");
        assertTrue(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));

        // en
        processor.languageFilterPatterns = List.of("en");
        assertTrue(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));

        // en or de
        processor.languageFilterPatterns = Arrays.asList("en", "de");
        assertTrue(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));

        // en or none
        processor.languageFilterPatterns = Arrays.asList("en", "");
        assertFalse(processor.isExcludedValue(ResourceFactory.createStringLiteral(lex)));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "en-us")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de")));
        assertTrue(processor.isExcludedValue(ResourceFactory.createLangLiteral(lex, "de-de")));
        assertFalse(processor.isExcludedValue(ResourceFactory.createResource()));
        assertFalse(processor.isExcludedValue(ResourceFactory.createTypedLiteral(1)));
    }

    @Test
    void compareResources() throws Exception {

        for (int i = 0; i < 10; i++) {
            this.addMapping(resource(i + 10), resource(i + 20));
        }

        assertSame(this.aspect1, resource(10), resource(20));

        assertDeviation(this.aspect1, resource(10), resource(21));

        assertMissing(this.aspect1, Collections.singletonList(resource(11)), List.of(), List.of(),
                Collections.singletonList(resource(11)), 0);
        assertMissing(this.aspect1, Arrays.asList(resource(11), resource(12)), List.of(), List.of(),
                Arrays.asList(resource(11), resource(12)), 0);
        assertMissing(this.aspect1, Arrays.asList(resource(11), resource(12)), Collections.singletonList(resource(21)),
                List.of(), Collections.singletonList(resource(12)), 1);

        assertDeviation(this.aspect1, Arrays.asList(resource(11), resource(12)),
                Arrays.asList(resource(21), resource(23)), Collections.singletonList(resource(11)), Collections.singletonList(resource(21)), 1);
    }

    private static class TestValueComparisonProcessor extends PropertyComparisonProcessor {

        public static TestValueComparisonProcessor getInstance() {
            TestValueComparisonProcessor processor = new TestValueComparisonProcessor();
            processor.variables = Collections.singletonList("value");
            processor.aspect = aspect(1);
            return processor;
        }

        @Override
        public boolean equivalentValues(RDFNode value1, RDFNode value2) {
            return Objects.equals(value1, value2);
        }
    }
}
