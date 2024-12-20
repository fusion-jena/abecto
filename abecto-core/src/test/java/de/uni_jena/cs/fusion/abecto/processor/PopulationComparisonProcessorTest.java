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

import static de.uni_jena.cs.fusion.abecto.Metadata.getQualityMeasurement;
import static de.uni_jena.cs.fusion.abecto.TestUtil.aspect;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsResourceDuplicate;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsResourceOmission;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.object;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.subject;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;

public class PopulationComparisonProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		Model inputPrimaryModel1 = ModelFactory.createDefaultModel()//
				.add(subject(111), property(1), object(1))//
				.add(subject(112), property(1), object(1))//
				.add(subject(113), property(1), object(1))//
				.add(subject(114), property(1), object(1))//
				.add(subject(121), property(2), object(2))//
				.add(subject(122), property(2), object(2))//
				.add(subject(123), property(2), object(2))//
				.add(subject(124), property(2), object(2))//
				.add(subject(131), property(3), object(3));
		Model inputPrimaryModel2 = ModelFactory.createDefaultModel()//
				.add(subject(211), property(1), object(1))//
				.add(subject(212), property(1), object(1))//
				.add(subject(221), property(2), object(2))//
				.add(subject(2211), property(2), object(2))//
				.add(subject(222), property(2), object(2))//
				.add(subject(232), property(3), object(3));
		Model inputPrimaryModel3 = ModelFactory.createDefaultModel()//
				.add(subject(315), property(1), object(1))//
				.add(subject(325), property(2), object(2))//
				.add(subject(333), property(3), object(3));

		Model inputGeneralMetaModel = ModelFactory.createDefaultModel();
		inputGeneralMetaModel.add(subject(111), AV.correspondsToResource, subject(211));
		inputGeneralMetaModel.add(subject(112), AV.correspondsToResource, subject(212));
		inputGeneralMetaModel.add(subject(121), AV.correspondsToResource, subject(221));
		inputGeneralMetaModel.add(subject(121), AV.correspondsToResource, subject(2211));
		inputGeneralMetaModel.add(subject(122), AV.correspondsToResource, subject(222));
		inputGeneralMetaModel.add(subject(221), AV.correspondsToResource, subject(2211));

		Query query1 = QueryFactory.create("SELECT ?key {?key <" + property(1) + "> <" + object(1) + ">}");
		Query query2 = QueryFactory.create("SELECT ?key {?key <" + property(2) + "> <" + object(2) + ">}");
		Query query3 = QueryFactory.create("SELECT ?key {?key <" + property(3) + "> <" + object(3) + ">}");

		Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), query1).setPattern(dataset(2), query1)
				.setPattern(dataset(3), query1);
		Aspect aspect2 = new Aspect(aspect(2), "key").setPattern(dataset(1), query2).setPattern(dataset(2), query2)
				.setPattern(dataset(3), query2);
		Aspect aspect3 = new Aspect(aspect(3), "key").setPattern(dataset(1), query3).setPattern(dataset(2), query3)
				.setPattern(dataset(3), query3);

		PopulationComparisonProcessor processor = new PopulationComparisonProcessor()
				.addInputPrimaryModel(dataset(1), inputPrimaryModel1)
				.addInputPrimaryModel(dataset(2), inputPrimaryModel2)
				.addInputPrimaryModel(dataset(3), inputPrimaryModel3)
				.addInputMetaModel(null, MappingProcessor.inferTransitiveCorrespondences(inputGeneralMetaModel))
				.addAspects(aspect1, aspect2, aspect3);
		processor.aspects = Arrays.asList(aspect(1), aspect(2), aspect(3));
		processor.run();
		Model outputMetaModelDataset1 = processor.getOutputMetaModel(dataset(1));
		Model outputMetaModelDataset2 = processor.getOutputMetaModel(dataset(2));
		Model outputMetaModelDataset3 = processor.getOutputMetaModel(dataset(3));

		// check absolute coverage
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset1).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(3), outputMetaModelDataset1).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(1), outputMetaModelDataset2).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(2), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(3), outputMetaModelDataset2).value);

		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(3), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(2), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(3), outputMetaModelDataset3).value);

		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset3).value);

		assertEquals(6, countMeasurement(AV.absoluteCoverage, outputMetaModelDataset1));
		assertEquals(6, countMeasurement(AV.absoluteCoverage, outputMetaModelDataset2));
		assertEquals(6, countMeasurement(AV.absoluteCoverage, outputMetaModelDataset3));

		// check relative coverage
		assertEquals(1d, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset1).value.doubleValue());
		assertEquals(1d, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset1).value.doubleValue());
		assertEquals(0.5, getQualityMeasurement(AV.relativeCoverage, dataset(2), null,
				Collections.singleton(dataset(1)), aspect(1), outputMetaModelDataset2).value.doubleValue());
		assertEquals(0.5, getQualityMeasurement(AV.relativeCoverage, dataset(2), null,
				Collections.singleton(dataset(1)), aspect(2), outputMetaModelDataset2).value.doubleValue());

		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(2), outputMetaModelDataset3).value);

		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset3).value);

		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(3), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(3), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(3), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(3), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(3), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.relativeCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(3), outputMetaModelDataset3).value);

		assertEquals(6, countMeasurement(AV.relativeCoverage, outputMetaModelDataset1));
		assertEquals(6, countMeasurement(AV.relativeCoverage, outputMetaModelDataset2));
		assertEquals(6, countMeasurement(AV.relativeCoverage, outputMetaModelDataset3));

		// check omissions
		assertTrue(containsResourceOmission(dataset(1), dataset(2), subject(232), aspect(3), outputMetaModelDataset1));
		assertTrue(containsResourceOmission(dataset(1), dataset(3), subject(315), aspect(1), outputMetaModelDataset1));
		assertTrue(containsResourceOmission(dataset(1), dataset(3), subject(325), aspect(2), outputMetaModelDataset1));
		assertTrue(containsResourceOmission(dataset(1), dataset(3), subject(333), aspect(3), outputMetaModelDataset1));

		assertTrue(containsResourceOmission(dataset(2), dataset(1), subject(113), aspect(1), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(1), subject(114), aspect(1), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(1), subject(123), aspect(2), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(1), subject(124), aspect(2), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(1), subject(131), aspect(3), outputMetaModelDataset2));

		assertTrue(containsResourceOmission(dataset(2), dataset(3), subject(315), aspect(1), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(3), subject(325), aspect(2), outputMetaModelDataset2));
		assertTrue(containsResourceOmission(dataset(2), dataset(3), subject(333), aspect(3), outputMetaModelDataset2));

		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(111), aspect(1), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(112), aspect(1), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(113), aspect(1), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(114), aspect(1), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(121), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(122), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(123), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(124), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(1), subject(131), aspect(3), outputMetaModelDataset3));

		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(211), aspect(1), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(212), aspect(1), outputMetaModelDataset3));

		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(221), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(2211), aspect(2), outputMetaModelDataset3));
		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(222), aspect(2), outputMetaModelDataset3));

		assertTrue(containsResourceOmission(dataset(3), dataset(2), subject(232), aspect(3), outputMetaModelDataset3));

		assertEquals(4, countResource(AV.ResourceOmission, outputMetaModelDataset1));
		assertEquals(8, countResource(AV.ResourceOmission, outputMetaModelDataset2));
		assertEquals(15, countResource(AV.ResourceOmission, outputMetaModelDataset3));

		// check duplicates
		assertTrue(containsResourceDuplicate(subject(221), subject(2211), aspect(2), outputMetaModelDataset2));
		assertTrue(containsResourceDuplicate(subject(2211), subject(221), aspect(2), outputMetaModelDataset2));

		assertEquals(0, countResource(AV.ResourceDuplicate, outputMetaModelDataset1));
		assertEquals(2, countResource(AV.ResourceDuplicate, outputMetaModelDataset2));
		assertEquals(0, countResource(AV.ResourceDuplicate, outputMetaModelDataset3));

		// check count
		assertEquals(4, getQualityMeasurement(AV.deduplicatedCount, dataset(1), null, Collections.emptyList(), aspect(1),
				outputMetaModelDataset1).value.intValue());
		assertEquals(4, getQualityMeasurement(AV.deduplicatedCount, dataset(1), null, Collections.emptyList(), aspect(2),
				outputMetaModelDataset1).value.intValue());
		assertEquals(1, getQualityMeasurement(AV.deduplicatedCount, dataset(1), null, Collections.emptyList(), aspect(3),
				outputMetaModelDataset1).value.intValue());
		assertEquals(2, getQualityMeasurement(AV.deduplicatedCount, dataset(2), null, Collections.emptyList(), aspect(1),
				outputMetaModelDataset2).value.intValue());
		assertEquals(2, getQualityMeasurement(AV.deduplicatedCount, dataset(2), null, Collections.emptyList(), aspect(2),
				outputMetaModelDataset2).value.intValue());
		assertEquals(1, getQualityMeasurement(AV.deduplicatedCount, dataset(2), null, Collections.emptyList(), aspect(3),
				outputMetaModelDataset2).value.intValue());
		assertEquals(1, getQualityMeasurement(AV.deduplicatedCount, dataset(3), null, Collections.emptyList(), aspect(1),
				outputMetaModelDataset3).value.intValue());
		assertEquals(1, getQualityMeasurement(AV.deduplicatedCount, dataset(3), null, Collections.emptyList(), aspect(2),
				outputMetaModelDataset3).value.intValue());
		assertEquals(1, getQualityMeasurement(AV.deduplicatedCount, dataset(3), null, Collections.emptyList(), aspect(3),
				outputMetaModelDataset3).value.intValue());

		assertEquals(3, countMeasurement(AV.deduplicatedCount, outputMetaModelDataset1));
		assertEquals(3, countMeasurement(AV.deduplicatedCount, outputMetaModelDataset2));
		assertEquals(3, countMeasurement(AV.deduplicatedCount, outputMetaModelDataset3));

		// check completeness
		assertEquals(0.5714285714285714, getQualityMeasurement(AV.marCompletenessThomas08, dataset(1), null,
				Arrays.asList(dataset(2), dataset(3)), aspect(1), outputMetaModelDataset1).value.doubleValue());
		assertEquals(0.5714285714285714, getQualityMeasurement(AV.marCompletenessThomas08, dataset(1), null,
				Arrays.asList(dataset(2), dataset(3)), aspect(2), outputMetaModelDataset1).value.doubleValue());
		assertEquals(0.2857142857142857, getQualityMeasurement(AV.marCompletenessThomas08, dataset(2), null,
				Arrays.asList(dataset(1), dataset(3)), aspect(1), outputMetaModelDataset2).value.doubleValue());
		assertEquals(0.2857142857142857, getQualityMeasurement(AV.marCompletenessThomas08, dataset(2), null,
				Arrays.asList(dataset(1), dataset(3)), aspect(2), outputMetaModelDataset2).value.doubleValue());
		assertEquals(0.1428571428571429, getQualityMeasurement(AV.marCompletenessThomas08, dataset(3), null,
				Arrays.asList(dataset(1), dataset(2)), aspect(1), outputMetaModelDataset3).value.doubleValue());
		assertEquals(0.1428571428571429, getQualityMeasurement(AV.marCompletenessThomas08, dataset(3), null,
				Arrays.asList(dataset(1), dataset(2)), aspect(2), outputMetaModelDataset3).value.doubleValue());

		assertThrows(NoSuchElementException.class, () -> getQualityMeasurement(AV.marCompletenessThomas08, dataset(1),
				null, Arrays.asList(dataset(2), dataset(3)), aspect(3), outputMetaModelDataset1));
		assertThrows(NoSuchElementException.class, () -> getQualityMeasurement(AV.marCompletenessThomas08, dataset(2),
				null, Arrays.asList(dataset(1), dataset(3)), aspect(3), outputMetaModelDataset2));
		assertThrows(NoSuchElementException.class, () -> getQualityMeasurement(AV.marCompletenessThomas08, dataset(3),
				null, Arrays.asList(dataset(1), dataset(2)), aspect(3), outputMetaModelDataset3));

		assertEquals(2, countMeasurement(AV.marCompletenessThomas08, outputMetaModelDataset1));
		assertEquals(2, countMeasurement(AV.marCompletenessThomas08, outputMetaModelDataset2));
		assertEquals(2, countMeasurement(AV.marCompletenessThomas08, outputMetaModelDataset3));
	}

	private static int countMeasurement(Resource measure, Model model) {
		return QueryExecutionFactory.create(
				QueryFactory.create(
						"SELECT (COUNT(*) AS  ?count) {?measurement <" + DQV.isMeasurementOf + "> <" + measure + ">}"),
				model).execSelect().next().getLiteral("count").getInt();
	}

	private static int countResource(Resource type, Model model) {
		return QueryExecutionFactory
				.create(QueryFactory.create("SELECT (COUNT(*) AS  ?count) {?resource a <" + type + ">}"), model)
				.execSelect().next().getLiteral("count").getInt();
	}

	Resource dataset1 = ResourceFactory.createResource("dataset1");
	Resource dataset2 = ResourceFactory.createResource("dataset2");
	Resource dataset3 = ResourceFactory.createResource("dataset3");
	Resource resource1OfD1 = ResourceFactory.createResource("resource1OfD1");
	Resource resource2OfD1 = ResourceFactory.createResource("resource2OfD1");
	Resource resource3OfD1 = ResourceFactory.createResource("resource3OfD1");
	Resource resource1OfD2 = ResourceFactory.createResource("resource1OfD2");
	Resource resource2OfD2 = ResourceFactory.createResource("resource2OfD2");
	Resource resource3OfD2 = ResourceFactory.createResource("resource3OfD2");
	Resource resource1OfD3 = ResourceFactory.createResource("resource1OfD3");
	Resource resource2OfD3 = ResourceFactory.createResource("resource2OfD3");
	Resource resource3OfD3 = ResourceFactory.createResource("resource3OfD3");

	PopulationComparisonProcessor processor;

	@BeforeEach
	public void initProcessor() {
		processor  = new PopulationComparisonProcessor();
		processor.datasets = Set.of(dataset1, dataset2, dataset3);
	}

	@Test
	public void incrementAbsoluteCoverednessOneEmpty() {
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Collections.emptySet(),
				dataset2, Set.of(resource1OfD2),
				dataset3, Set.of(resource1OfD3)
		));
		assertNull(processor.absoluteCoveredness.get(dataset1));
		assertEquals(1, processor.absoluteCoveredness.get(dataset2));
		assertEquals(1, processor.absoluteCoveredness.get(dataset3));
	}

	@Test
	public void incrementAbsoluteCoverednessAllOne() {
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Set.of(resource1OfD1),
				dataset2, Set.of(resource1OfD2),
				dataset3, Set.of(resource1OfD3)
		));
		assertEquals(1, processor.absoluteCoveredness.get(dataset1));
		assertEquals(1, processor.absoluteCoveredness.get(dataset2));
		assertEquals(1, processor.absoluteCoveredness.get(dataset3));
	}

	@Test
	public void incrementAbsoluteCoverednessAllMultiple() {
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Set.of(resource1OfD1,resource2OfD1,resource3OfD1),
				dataset2, Set.of(resource1OfD2,resource2OfD2,resource3OfD2),
				dataset3, Set.of(resource1OfD3,resource2OfD3,resource3OfD3)
		));
		assertEquals(1, processor.absoluteCoveredness.get(dataset1));
		assertEquals(1, processor.absoluteCoveredness.get(dataset2));
		assertEquals(1, processor.absoluteCoveredness.get(dataset3));
	}

	@Test
	public void incrementAbsoluteCoverednessMultipleInvocations() {
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Collections.emptySet(),
				dataset2, Set.of(resource1OfD2),
				dataset3, Set.of(resource1OfD3)
		));
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Set.of(resource1OfD1,resource2OfD1,resource3OfD1),
				dataset2, Collections.emptySet(),
				dataset3, Set.of(resource1OfD3)
		));
		assertEquals(1, processor.absoluteCoveredness.get(dataset1));
		assertEquals(1, processor.absoluteCoveredness.get(dataset2));
		assertEquals(2, processor.absoluteCoveredness.get(dataset3));
	}

	@Test
	public void incrementAbsoluteCoverednessOnlyOneDuplicate() {
		processor.incrementAbsoluteCoveredness(Map.of(
				dataset1, Set.of(resource1OfD1,resource2OfD1),
				dataset2, Collections.emptySet(),
				dataset3, Collections.emptySet()
		));
		assertNull(processor.absoluteCoveredness.get(dataset1));
		assertNull(processor.absoluteCoveredness.get(dataset2));
		assertNull(processor.absoluteCoveredness.get(dataset3));
	}

}
