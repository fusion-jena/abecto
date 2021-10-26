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

import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;

import java.util.Arrays;
import java.util.Collection;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiteralValueComparisonProcessorTest extends AbstractValueComparisonProcessorTest {

	@BeforeAll
	public static void initJena() {
		JenaSystem.init();
	}

	@BeforeEach
	public void initMapping() {
		for (int i = 0; i < 10; i++) {
			this.addMapping(resource(1), resource(2));
		}
	}

	@Override
	public Processor<?> getInstance(Collection<String> variables, Resource aspect) {
		LiteralValueComparisonProcessor processor = new LiteralValueComparisonProcessor();
		processor.variables = variables;
		processor.aspect = aspect;
		return processor;
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

		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("value1", null), resource("otherEntity"),
				"Should be a literal.");
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean),
				resource("otherEntity"), "Should be a literal.");
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger),
				resource("otherEntity"), "Should be a literal.");
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal),
				resource("otherEntity"), "Should be a literal.");
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDfloat),
				resource("otherEntity"), "Should be a literal.");
		assertUnexpectedValueType(ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDdouble),
				resource("otherEntity"), "Should be a literal.");

		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.001", XSDDatatype.XSDfloat),
				ResourceFactory.createTypedLiteral("0.001e0", XSDDatatype.XSDdouble));
		assertDeviation(//
				ResourceFactory.createTypedLiteral("0.001", XSDDatatype.XSDdouble),
				ResourceFactory.createTypedLiteral("0.001e0", XSDDatatype.XSDfloat));

		Literal one = ResourceFactory.createTypedLiteral("1", XSDDatatype.XSDinteger);
		Literal two = ResourceFactory.createTypedLiteral("2", XSDDatatype.XSDinteger);
		Literal three = ResourceFactory.createTypedLiteral("3", XSDDatatype.XSDinteger);
		assertMissing(Arrays.asList(one), Arrays.asList(), Arrays.asList(), Arrays.asList(one));
		assertMissing(Arrays.asList(one, two), Arrays.asList(), Arrays.asList(), Arrays.asList(one, two));
		assertMissing(Arrays.asList(one, two), Arrays.asList(one), Arrays.asList(), Arrays.asList(two));

		// deviation if same present
		assertDeviation(Arrays.asList(one, two), Arrays.asList(one, three), Arrays.asList(one), Arrays.asList(one));

	}
}