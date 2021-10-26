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

import static de.uni_jena.cs.fusion.abecto.TestUtil.aspect;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsDeviation;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsIssue;
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsValuesOmission;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static de.uni_jena.cs.fusion.abecto.TestUtil.subject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public abstract class AbstractValueComparisonProcessorTest {

	Query pattern = QueryFactory.create("SELECT ?key ?value ?dummy WHERE {OPTIONAL{?key <" + property(1)
			+ "> ?value} ?key <" + property(2) + "> ?dummy}");
	Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
	Model mappingModel = ModelFactory.createDefaultModel();
	{
		addMapping(subject(1), subject(2));
	}

	public void addMapping(Resource... resources) {
		Correspondences.addCorrespondence(mappingModel, mappingModel, aspect(1), resources);
	}

	public abstract Processor<?> getInstance(Collection<String> variables, Resource aspect);

	Model[] compare(Model model1, Model model2) throws Exception {
		Processor<?> processor = getInstance(Collections.singletonList("value"), aspect(1))
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2)
				.addInputMetaModels(null, Collections.singleton(mappingModel)).setAspectMap(Map.of(aspect(1), aspect1));
		processor.run();
		return new Model[] { processor.getOutputMetaModel(dataset(1)), processor.getOutputMetaModel(dataset(2)) };
	}

	void assertUnexpectedValueType(RDFNode expectedValue, RDFNode unexpectedValue, String issueComment)
			throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), unexpectedValue);
		Model model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), expectedValue);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertTrue(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		assertTrue(containsIssue(subject(1), "value", unexpectedValue, aspect(1), "Invalid Value", issueComment,
				outputMetaModels[0]));
		assertEquals(1, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(0, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());

		// second direction
		model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), expectedValue);
		model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), unexpectedValue);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertTrue(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		assertTrue(containsIssue(subject(2), "value", unexpectedValue, aspect(1), "Invalid Value", issueComment,
				outputMetaModels[1]));
		assertEquals(0, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(1, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());
	}

	void assertDeviation(RDFNode value1, RDFNode value2) throws Exception {
		assertDeviation(Collections.singleton(value1), Collections.singleton(value2), Collections.emptyList(),
				Collections.emptyList());
	}

	void assertDeviation(Collection<RDFNode> values1, Collection<RDFNode> values2,
			Collection<RDFNode> notDeviatingValues1, Collection<RDFNode> notDeviatingValues2) throws Exception {
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
		Model[] outputMetaModels = compare(model1, model2);
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

		// second direction
		model1 = ModelFactory.createDefaultModel();
		model2 = ModelFactory.createDefaultModel();
		for (RDFNode value2 : values2) {
			model1.add(subject(1), property(1), value2);
		}
		for (RDFNode value1 : values1) {
			model2.add(subject(2), property(1), value1);
		}
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		for (RDFNode value1 : values1) {
			if (!notDeviatingValues1.contains(value1)) {
				for (RDFNode value2 : values2) {
					if (!notDeviatingValues2.contains(value2)) {
						assertTrue(containsDeviation(subject(1), "value", value2, dataset(2), subject(2), value1,
								aspect(1), outputMetaModels[0]));
						assertTrue(containsDeviation(subject(2), "value", value1, dataset(1), subject(1), value2,
								aspect(1), outputMetaModels[1]));
					}
				}
			}
		}
		assertEquals(expectedDeviationCount,
				outputMetaModels[0].listStatements(null, RDF.type, AV.Deviation).toList().size());
		assertEquals(expectedDeviationCount,
				outputMetaModels[1].listStatements(null, RDF.type, AV.Deviation).toList().size());
	}

	void assertSame(RDFNode value1, RDFNode value2) throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), value1);
		Model model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), value2);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		Model[] outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));

		// second direction
		model1 = ModelFactory.createDefaultModel().add(subject(1), property(1), value2);
		model2 = ModelFactory.createDefaultModel().add(subject(2), property(1), value1);
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.ValueOmission));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.ValueOmission));
	}

	void assertMissing(Collection<RDFNode> values1, Collection<RDFNode> values2, Collection<RDFNode> missingValues1,
			Collection<RDFNode> missingValues2) throws Exception {
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
		Model[] outputMetaModels = compare(model1, model2);
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

		// second direction
		model1 = ModelFactory.createDefaultModel();
		model2 = ModelFactory.createDefaultModel();
		for (RDFNode value2 : values2) {
			model1.add(subject(1), property(1), value2);
		}
		for (RDFNode value1 : values1) {
			model2.add(subject(2), property(1), value1);
		}
		model1.add(subject(1), property(2), resource("alwaysPresent"));
		model2.add(subject(2), property(2), resource("alwaysPresent"));
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
		assertEquals(missingValues2.size(),
				outputMetaModels[0].listStatements(null, RDF.type, AV.ValueOmission).toList().size());
		assertEquals(missingValues1.size(),
				outputMetaModels[1].listStatements(null, RDF.type, AV.ValueOmission).toList().size());
		for (RDFNode missingValue2 : missingValues2) {
			assertTrue(containsValuesOmission(subject(1), "value", dataset(2), subject(2), missingValue2, aspect(1),
					outputMetaModels[0]));
		}
		for (RDFNode missingValue1 : missingValues1) {
			assertTrue(containsValuesOmission(subject(2), "value", dataset(1), subject(1), missingValue1, aspect(1),
					outputMetaModels[1]));
		}
	}
}
