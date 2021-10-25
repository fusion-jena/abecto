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
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

class LiteralValueComparisonProcessorTest {

	@BeforeAll
	public static void initJena() {
		JenaSystem.init();
	}

	Query pattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property(1) + "> ?value .}");
	Aspect aspect1 = new Aspect(aspect(1), "key").setPattern(dataset(1), pattern).setPattern(dataset(2), pattern);
	Model mappingModel = ModelFactory.createDefaultModel();
	{
		Correspondences.addCorrespondence(mappingModel, mappingModel, aspect(1), resource(1), resource(2));
	}

	Model[] compare(Model model1, Model model2) throws Exception {
		LiteralValueComparisonProcessor processor = new LiteralValueComparisonProcessor()
				.addInputPrimaryModel(dataset(1), model1).addInputPrimaryModel(dataset(2), model2)
				.addInputMetaModels(null, Collections.singleton(mappingModel)).setAspectMap(Map.of(aspect(1), aspect1));
		processor.variables = Collections.singletonList("value");
		processor.aspect = aspect(1);
		processor.run();
		return new Model[] { processor.getOutputMetaModel(dataset(1)), processor.getOutputMetaModel(dataset(2)) };
	}

	void assertUnexpectedValueType(Literal value) throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), resource("otherEntity"));
		Model model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), value.getLexicalForm(),
				value.getDatatype());
		Model[] outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));

		assertTrue(containsIssue(resource(1), "value", resource("otherEntity"), aspect(1), "Invalid Value",
				"Should be a literal.", outputMetaModels[0]));
		assertEquals(1, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(0, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());

		// second direction
		model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), value.getLexicalForm(),
				value.getDatatype());
		model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), resource("otherEntity"));
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));

		assertTrue(containsIssue(resource(2), "value", resource("otherEntity"), aspect(1), "Invalid Value",
				"Should be a literal.", outputMetaModels[1]));
		assertEquals(0, outputMetaModels[0].listStatements(null, RDF.type, AV.Issue).toList().size());
		assertEquals(1, outputMetaModels[1].listStatements(null, RDF.type, AV.Issue).toList().size());
	}

	void assertDeviation(Literal value1, Literal value2) throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), value1.getLexicalForm(),
				value1.getDatatype());
		Model model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), value2.getLexicalForm(),
				value2.getDatatype());
		Model[] outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));

		assertTrue(containsDeviation(resource(1), "value", value1, dataset(2), resource(2), value2, aspect(1),
				outputMetaModels[0]));
		assertTrue(containsDeviation(resource(2), "value", value2, dataset(1), resource(1), value1, aspect(1),
				outputMetaModels[1]));
		assertEquals(1, outputMetaModels[0].listStatements(null, RDF.type, AV.Deviation).toList().size());
		assertEquals(1, outputMetaModels[1].listStatements(null, RDF.type, AV.Deviation).toList().size());

		// second direction
		model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), value2.getLexicalForm(),
				value2.getDatatype());
		model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), value1.getLexicalForm(),
				value1.getDatatype());
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));

		assertTrue(containsDeviation(resource(1), "value", value2, dataset(2), resource(2), value1, aspect(1),
				outputMetaModels[0]));
		assertTrue(containsDeviation(resource(2), "value", value1, dataset(1), resource(1), value2, aspect(1),
				outputMetaModels[1]));
		assertEquals(1, outputMetaModels[0].listStatements(null, RDF.type, AV.Deviation).toList().size());
		assertEquals(1, outputMetaModels[1].listStatements(null, RDF.type, AV.Deviation).toList().size());
	}

	void assertSame(Literal value1, Literal value2) throws Exception {
		// first direction
		Model model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), value1.getLexicalForm(),
				value1.getDatatype());
		Model model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), value2.getLexicalForm(),
				value2.getDatatype());
		Model[] outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));

		// second direction
		model1 = ModelFactory.createDefaultModel().add(resource(1), property(1), value2.getLexicalForm(),
				value2.getDatatype());
		model2 = ModelFactory.createDefaultModel().add(resource(2), property(1), value1.getLexicalForm(),
				value1.getDatatype());
		outputMetaModels = compare(model1, model2);
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Issue));
		assertFalse(outputMetaModels[0].contains(null, RDF.type, AV.Deviation));
		assertFalse(outputMetaModels[1].contains(null, RDF.type, AV.Deviation));
	}

	@Test
	void computeResultModel() throws Exception {
		assertSame(//
				ResourceFactory.createTypedLiteral("value", null), //
				ResourceFactory.createTypedLiteral("value", null));

		assertSame(//
				ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean),
				ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean));

		assertSame(//
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger));
		assertSame(//
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDdecimal));
		assertSame(//
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDdouble));

		assertSame(//
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdecimal));
		assertSame(//
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdouble));

		assertSame(//
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("0.0042", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042E0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("0.0042", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042e0", XSDDatatype.XSDfloat));
		assertSame(//
				ResourceFactory.createTypedLiteral("0.0042", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-3", XSDDatatype.XSDfloat));

		assertSame(//
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));

		assertSame(//
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("value1", null), ResourceFactory.createTypedLiteral("value2", null));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean),
				ResourceFactory.createTypedLiteral("false", XSDDatatype.XSDboolean));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDinteger));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDdecimal));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger),
				ResourceFactory.createTypedLiteral("-5", XSDDatatype.XSDdouble));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdecimal));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal),
				ResourceFactory.createTypedLiteral("-5.0", XSDDatatype.XSDdouble));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-3", XSDDatatype.XSDfloat));

		// float and double
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.0042e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("4.2e-3", XSDDatatype.XSDdouble));

		// double and float
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.0042E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.0042e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e-3", XSDDatatype.XSDfloat));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e0", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2E-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("3.2e-9", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.0042E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.0042e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.0032", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("4.2e-3", XSDDatatype.XSDdouble));

		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("value1", null));
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean));
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger));
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal));
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDfloat));
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDdouble));

		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.001", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.001e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.001", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.001e0", XSDDatatype.XSDfloat));
	}

}
