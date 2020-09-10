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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class LiteralDeviationProcessorTest {
	static UUID id1 = UUID.randomUUID();
	static UUID id2 = UUID.randomUUID();

	static Resource entity1 = ResourceFactory.createResource("http://example.org/1/entity");
	static Resource entity2 = ResourceFactory.createResource("http://example.org/2/entity");
	static Resource otherEntity = ResourceFactory.createResource("http://example.org/otherEntity");
	static Property propterty = ResourceFactory.createProperty("http://example.org/property");

	Model compare(Model model1, Model model2) throws Exception {
		// ensure Jena initialization
		JenaSystem.init();

		// prepare categories and mappings
		Model metaModel = Models.getEmptyOntModel();
		String categoryTemplate = "{?entity <" + propterty.getURI() + "> ?value .}";
		SparqlEntityManager.insert(Arrays.asList(//
				new Category("entity", String.format(categoryTemplate, 1), id1), //
				new Category("entity", String.format(categoryTemplate, 2), id2)), metaModel);
		SparqlEntityManager.insert(Arrays.asList(Mapping.of(//
				entity1, entity2)), metaModel);

		// execute LiteralDeviationProcessorTest
		LiteralDeviationProcessor processor = new LiteralDeviationProcessor();
		LiteralDeviationProcessor.Parameter parameter = new LiteralDeviationProcessor.Parameter();
		parameter.variables = Collections.singletonMap("entity", Arrays.asList("value"));
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		return processor.call();
	}

	void assertUnexpectedValueType(LiteralLabel value) throws Exception {
		Model metaModel;
		Model model1;
		Model model2;
		Collection<Deviation> deviations;
		Collection<Issue> issues;

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, otherEntity);
		model2.add(entity2, propterty, model1.createTypedLiteral(value.getLexicalForm(), value.getDatatype()));
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(0, deviations.size());
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, entity1, "value", "literal")));
		assertEquals(1, issues.size());

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, model1.createTypedLiteral(value.getLexicalForm(), value.getDatatype()));
		model2.add(entity2, propterty, otherEntity);
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(0, deviations.size());
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, entity2, "value", "literal")));
		assertEquals(1, issues.size());
	}

	void assertDeviation(LiteralLabel value1, LiteralLabel value2) throws Exception {
		Model metaModel;
		Model model1;
		Model model2;
		Collection<Deviation> deviations;
		Deviation deviation;
		Collection<Issue> issues;

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, model1.createTypedLiteral(value1.getLexicalForm(), value1.getDatatype()));
		model2.add(entity2, propterty, model2.createTypedLiteral(value2.getLexicalForm(), value2.getDatatype()));
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(1, deviations.size());
		deviation = deviations.iterator().next();
		assertEquals("entity", deviation.categoryName);
		assertEquals("value", deviation.variableName);
		assertEquals(entity1, deviation.resource1);
		assertEquals(entity2, deviation.resource2);
		assertEquals(id1, deviation.ontologyId1);
		assertEquals(id2, deviation.ontologyId2);
		assertEquals(value1.toString(), deviation.value1);
		assertEquals(value2.toString(), deviation.value2);
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertEquals(0, issues.size());

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, model1.createTypedLiteral(value2.getLexicalForm(), value2.getDatatype()));
		model2.add(entity2, propterty, model2.createTypedLiteral(value1.getLexicalForm(), value1.getDatatype()));
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(1, deviations.size());
		deviation = deviations.iterator().next();
		assertEquals("entity", deviation.categoryName);
		assertEquals("value", deviation.variableName);
		assertEquals(entity1, deviation.resource1);
		assertEquals(entity2, deviation.resource2);
		assertEquals(id1, deviation.ontologyId1);
		assertEquals(id2, deviation.ontologyId2);
		assertEquals(value2.toString(), deviation.value1);
		assertEquals(value1.toString(), deviation.value2);
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertEquals(0, issues.size());
	}

	void assertSame(LiteralLabel value1, LiteralLabel value2) throws Exception {
		Model metaModel;
		Model model1;
		Model model2;
		Collection<Deviation> deviations;
		Collection<Issue> issues;

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, model1.createTypedLiteral(value1.getLexicalForm(), value1.getDatatype()));
		model2.add(entity2, propterty, model2.createTypedLiteral(value2.getLexicalForm(), value2.getDatatype()));
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(0, deviations.size());
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertEquals(0, issues.size());

		model1 = Models.getEmptyOntModel();
		model2 = Models.getEmptyOntModel();
		model1.add(entity1, propterty, model1.createTypedLiteral(value2.getLexicalForm(), value2.getDatatype()));
		model2.add(entity2, propterty, model2.createTypedLiteral(value1.getLexicalForm(), value1.getDatatype()));
		metaModel = compare(model1, model2);
		deviations = SparqlEntityManager.select(new Deviation(null, null, null, null, null, id1, id2, null, null),
				metaModel);
		assertEquals(0, deviations.size());
		issues = SparqlEntityManager.select(new Issue(), metaModel);
		assertEquals(0, issues.size());
	}

	public static LiteralLabel literalLabel(String value, RDFDatatype type) {
		return LiteralLabelFactory.create(value, type);
	}

	@Test
	void computeResultModel() throws Exception {
		assertSame(//
				LiteralLabelFactory.create("value", (String) null), //
				LiteralLabelFactory.create("value", (String) null));

		assertSame(//
				LiteralLabelFactory.create("true", XSDDatatype.XSDboolean),
				LiteralLabelFactory.create("true", XSDDatatype.XSDboolean));

		assertSame(//
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger));
		assertSame(//
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDdecimal));
		assertSame(//
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDdouble));

		assertSame(//
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdecimal));
		assertSame(//
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdouble));

		assertSame(//
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("0.0042", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042E0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("0.0042", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042e0", XSDDatatype.XSDfloat));
		assertSame(//
				LiteralLabelFactory.create("0.0042", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-3", XSDDatatype.XSDfloat));

		assertSame(//
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));

		assertSame(//
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));

		assertDeviation(//
				LiteralLabelFactory.create("value1", (String) null),
				LiteralLabelFactory.create("value2", (String) null));

		assertDeviation(//
				LiteralLabelFactory.create("true", XSDDatatype.XSDboolean),
				LiteralLabelFactory.create("false", XSDDatatype.XSDboolean));

		assertDeviation(//
				LiteralLabelFactory.create("-4", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDinteger));
		assertDeviation(//
				LiteralLabelFactory.create("-4", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDdecimal));
		assertDeviation(//
				LiteralLabelFactory.create("-4", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("-4", XSDDatatype.XSDinteger),
				LiteralLabelFactory.create("-5", XSDDatatype.XSDdouble));

		assertDeviation(//
				LiteralLabelFactory.create("-4.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdecimal));
		assertDeviation(//
				LiteralLabelFactory.create("-4.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("-4.0", XSDDatatype.XSDdecimal),
				LiteralLabelFactory.create("-5.0", XSDDatatype.XSDdouble));

		assertDeviation(//
				LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-3", XSDDatatype.XSDfloat));

		// float and double
		assertDeviation(//
				LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.0042e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("4.2e-3", XSDDatatype.XSDdouble));

		// double and float
		assertDeviation(//
				LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("0.0042E0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("0.0042e0", XSDDatatype.XSDfloat));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e-3", XSDDatatype.XSDfloat));

		assertDeviation(//
				LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e0", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2E-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2E-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("3.2e-9", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e-9", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("0.0042E0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("0.0042e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.0032", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("4.2e-3", XSDDatatype.XSDdouble));

		assertUnexpectedValueType(LiteralLabelFactory.create("value1", (String) null));
		assertUnexpectedValueType(LiteralLabelFactory.create("true", XSDDatatype.XSDboolean));
		assertUnexpectedValueType(LiteralLabelFactory.create("-4", XSDDatatype.XSDinteger));
		assertUnexpectedValueType(LiteralLabelFactory.create("-4.0", XSDDatatype.XSDdecimal));
		assertUnexpectedValueType(LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDfloat));
		assertUnexpectedValueType(LiteralLabelFactory.create("3.2E9", XSDDatatype.XSDdouble));

		assertDeviation(//
				LiteralLabelFactory.create("0.001", XSDDatatype.XSDfloat),
				LiteralLabelFactory.create("0.001e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				LiteralLabelFactory.create("0.001", XSDDatatype.XSDdouble),
				LiteralLabelFactory.create("0.001e0", XSDDatatype.XSDfloat));
	}

}
