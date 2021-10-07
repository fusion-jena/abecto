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
package de.uni_jena.cs.fusion.abecto.processor;

import static de.uni_jena.cs.fusion.abecto.Correspondences.addCorrespondence;
import static de.uni_jena.cs.fusion.abecto.Metadata.getQualityMeasurement;
import static de.uni_jena.cs.fusion.abecto.TestUtil.aspect;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsIssue;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.object;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.subject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class CompletenessProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// TODO rewrite

		Model inputPrimaryModel1 = ModelFactory.createDefaultModel()//
				.add(subject(111), property(1), object(1))//
				.add(subject(112), property(1), object(1))//
				.add(subject(113), property(1), object(1))//
				.add(subject(114), property(1), object(1))//
				.add(subject(121), property(2), object(2))//
				.add(subject(122), property(2), object(2))//
				.add(subject(123), property(2), object(2))//
				.add(subject(124), property(2), object(2));
		Model inputPrimaryModel2 = ModelFactory.createDefaultModel()//
				.add(subject(211), property(1), object(1))//
				.add(subject(212), property(1), object(1))//
				.add(subject(221), property(2), object(2))//
				.add(subject(2211), property(2), object(2))//
				.add(subject(222), property(2), object(2));
		Model inputPrimaryModel3 = ModelFactory.createDefaultModel()//
				.add(subject(315), property(1), object(1))//
				.add(subject(325), property(2), object(2));

		Model inputGeneralMetaModel = ModelFactory.createDefaultModel();
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(1), subject(111), subject(211));
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(1), subject(112), subject(212));
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(2), subject(121), subject(221));
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(2), subject(121), subject(2211));
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(2), subject(122), subject(222));
		addCorrespondence(inputGeneralMetaModel, inputGeneralMetaModel, aspect(2), subject(221), subject(2211));

		Query query1 = QueryFactory.create("SELECT ?key {?key <" + property(1) + "> <" + object(1) + ">}");
		Query query2 = QueryFactory.create("SELECT ?key {?key <" + property(2) + "> <" + object(2) + ">}");

		Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), query1).setPattern(dataset(2), query1)
				.setPattern(dataset(3), query1);
		Aspect aspect2 = new Aspect(aspect(2), "key").setPattern(dataset(1), query2).setPattern(dataset(2), query2)
				.setPattern(dataset(3), query2);

		CompletenessProcessor processor = new CompletenessProcessor()
				.addInputPrimaryModel(dataset(1), inputPrimaryModel1)
				.addInputPrimaryModel(dataset(2), inputPrimaryModel2)
				.addInputPrimaryModel(dataset(3), inputPrimaryModel3).addInputMetaModel(null, inputGeneralMetaModel)
				.setAspectMap(Map.of(aspect(1), aspect1, aspect(2), aspect2));
		processor.aspects = Arrays.asList(aspect(1), aspect(2));
		processor.run();
		Model outputMetaModelDataset1 = processor.getOutputMetaModel(dataset(1));
		Model outputMetaModelDataset2 = processor.getOutputMetaModel(dataset(2));
		Model outputMetaModelDataset3 = processor.getOutputMetaModel(dataset(3));

//		System.out.println("\n\n\n################ Dataset 1 #################\n\n\n");
//		outputMetaModelDataset1.write(System.out, "TTL");
//
//		System.out.println("\n\n\n################ Dataset 2 #################\n\n\n");
//		outputMetaModelDataset2.write(System.out, "TTL");
//
//		System.out.println("\n\n\n################ Dataset 3 #################\n\n\n");
//		outputMetaModelDataset3.write(System.out, "TTL");

		// check absolute coverage
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset1).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset1).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(1), outputMetaModelDataset2).value);
		assertEquals(2, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(1)),
				aspect(2), outputMetaModelDataset2).value);

		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(1), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset1).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(1)),
				aspect(2), outputMetaModelDataset3).value);

		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(1), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(2), null, Collections.singleton(dataset(3)),
				aspect(2), outputMetaModelDataset2).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(1), outputMetaModelDataset3).value);
		assertEquals(0, getQualityMeasurement(AV.absoluteCoverage, dataset(3), null, Collections.singleton(dataset(2)),
				aspect(2), outputMetaModelDataset3).value);

//		assertEquals(12,
//				SparqlEntityManager
//						.select(new Measurement(null, null, "Coverage (absolute)", null, null, null, null, null),
//								outputGeneralMetaModel)
//						.size());

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

//		assertEquals(12,
//				SparqlEntityManager
//						.select(new Measurement(null, null, "Coverage (relative in %)", null, null, null, null, null),
//								outputGeneralMetaModel)
//						.size());

		// check omissions
//		assertTrue(omission(aspect(1), dataset(1), dataset(2), 113, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(1), dataset(2), 114, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(2), 123, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(2), 124, outputGeneralMetaModel).isPresent());
//
//		assertTrue(omission(aspect(1), dataset(1), dataset(3), 111, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(1), dataset(3), 112, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(1), dataset(3), 113, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(1), dataset(3), 114, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(3), dataset(1), 315, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(3), 121, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(3), 122, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(3), 123, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(1), dataset(3), 124, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(3), dataset(1), 325, outputGeneralMetaModel).isPresent());
//
//		assertTrue(omission(aspect(1), dataset(2), dataset(3), 211, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(2), dataset(3), 212, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(1), dataset(3), dataset(2), 315, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(2), dataset(3), 221, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(2), dataset(3), 2211, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(2), dataset(3), 222, outputGeneralMetaModel).isPresent());
//		assertTrue(omission(aspect(2), dataset(3), dataset(2), 325, outputGeneralMetaModel).isPresent());

//		assertEquals(21,
//				SparqlEntityManager.select(new Omission(null, null, null, null, null), outputGeneralMetaModel).size());

		// check duplicates
		assertTrue(containsIssue(subject(221), null, null, aspect(2), "Duplicated Resource",
				"of <" + subject(2211) + ">", outputMetaModelDataset2));
		assertTrue(containsIssue(subject(2211), null, null, aspect(2), "Duplicated Resource",
				"of <" + subject(221) + ">", outputMetaModelDataset2));

//		assertEquals(2, SparqlEntityManager.select(new Issue(), outputGeneralMetaModel).size());

		// TODO check completeness

		// TODO check parameter use
		
		// TODO check divide zero handling
	}

}
