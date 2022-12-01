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

import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LiteralValueComparisonProcessorTest extends AbstractValueComparisonProcessorTest {

	@BeforeAll
	public static void initJena() {
		JenaSystem.init();
	}

	@Override
	public Processor<?> getInstance(List<String> variables, Resource aspect) {
		LiteralValueComparisonProcessor processor = new LiteralValueComparisonProcessor();
		processor.variables = variables;
		processor.aspect = aspect;
		return processor;
	}

	@Test
	public void allowLangTagSkip() {
		LiteralValueComparisonProcessor processor = new LiteralValueComparisonProcessor();
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
	void useValue() {
		LiteralValueComparisonProcessor processor = new LiteralValueComparisonProcessor();
		String lex = "";

		// empty
		processor.languageFilterPatterns = Arrays.asList();
		assertTrue(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));

		// none
		processor.languageFilterPatterns = Arrays.asList("");
		assertTrue(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));

		// any
		processor.languageFilterPatterns = Arrays.asList("*");
		assertFalse(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));

		// en
		processor.languageFilterPatterns = Arrays.asList("en");
		assertFalse(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));

		// en or de
		processor.languageFilterPatterns = Arrays.asList("en", "de");
		assertFalse(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));

		// en or none
		processor.languageFilterPatterns = Arrays.asList("en", "");
		assertTrue(processor.useValue(ResourceFactory.createStringLiteral(lex)));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en")));
		assertTrue(processor.useValue(ResourceFactory.createLangLiteral(lex, "en-us")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de")));
		assertFalse(processor.useValue(ResourceFactory.createLangLiteral(lex, "de-de")));
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

		assertUnexpectedValueType(ResourceFactory.createStringLiteral("value1"), resource("otherEntity"),
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
		assertMissing(Arrays.asList(one), Arrays.asList(), Arrays.asList(), Arrays.asList(one), 0);
		assertMissing(Arrays.asList(one, two), Arrays.asList(), Arrays.asList(), Arrays.asList(one, two), 0);
		assertMissing(Arrays.asList(one, two), Arrays.asList(one), Arrays.asList(), Arrays.asList(two), 1);

		// deviation if same present
		assertDeviation(Arrays.asList(one, two), Arrays.asList(one, three), Arrays.asList(one), Arrays.asList(one), 1);

	}
}
