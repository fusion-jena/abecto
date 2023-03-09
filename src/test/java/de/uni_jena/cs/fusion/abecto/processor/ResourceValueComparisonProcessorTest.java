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

import java.util.Arrays;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceValueComparisonProcessorTest extends AbstractValueComparisonProcessorTest {
	@BeforeAll
	public static void initJena() {
		JenaSystem.init();

	}

	@BeforeEach
	public void initMapping() {
		for (int i = 0; i < 10; i++) {
			this.addMapping(resource(i + 10), resource(i + 20));
		}
	}

	@Override
	public Processor<?> getInstance(List<String> variables, Resource aspect) {
		ResourceValueComparisonProcessor processor = new ResourceValueComparisonProcessor();
		processor.variables = variables;
		processor.aspect = aspect;
		return processor;
	}

	@Test
	void computeResultModel() throws Exception {

		assertSame(this.aspect1, resource(10), resource(20));

		assertDeviation(this.aspect1, resource(10), resource(21));

		assertUnexpectedValueType(this.aspect1, resource(10), ResourceFactory.createTypedLiteral("value1", null),
				"Should be a resource.");
		assertUnexpectedValueType(this.aspect1, resource(10),
				ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean), "Should be a resource.");
		assertUnexpectedValueType(this.aspect1, resource(10),
				ResourceFactory.createTypedLiteral("-4", XSDDatatype.XSDinteger), "Should be a resource.");
		assertUnexpectedValueType(this.aspect1, resource(10),
				ResourceFactory.createTypedLiteral("-4.0", XSDDatatype.XSDdecimal), "Should be a resource.");
		assertUnexpectedValueType(this.aspect1, resource(10),
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDfloat), "Should be a resource.");
		assertUnexpectedValueType(this.aspect1, resource(10),
				ResourceFactory.createTypedLiteral("3.2E9", XSDDatatype.XSDdouble), "Should be a resource.");

		assertMissing(this.aspect1, Arrays.asList(resource(11)), Arrays.asList(), Arrays.asList(),
				Arrays.asList(resource(11)), 0);
		assertMissing(this.aspect1, Arrays.asList(resource(11), resource(12)), Arrays.asList(), Arrays.asList(),
				Arrays.asList(resource(11), resource(12)), 0);
		assertMissing(this.aspect1, Arrays.asList(resource(11), resource(12)), Arrays.asList(resource(21)),
				Arrays.asList(), Arrays.asList(resource(12)), 1);

		assertDeviation(this.aspect1, Arrays.asList(resource(11), resource(12)),
				Arrays.asList(resource(21), resource(23)), Arrays.asList(resource(11)), Arrays.asList(resource(21)), 1);
	}
}
