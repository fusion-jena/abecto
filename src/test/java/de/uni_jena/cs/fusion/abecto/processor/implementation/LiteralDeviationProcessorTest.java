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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
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

	@Test
	void computeResultModel() throws Exception {
		JenaSystem.init();

		// preparation
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/1/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"//

				+ ":right   :string       \"value1\"                 .\n"//
				+ ":right   :integer      \"-5\"^^xsd:integer        .\n"//
				+ ":right   :decimal      \"-5.0\"^^xsd:decimal      .\n"//
				+ ":right   :float        \"4.2E9\"^^xsd:float       .\n"//
				+ ":right   :double       \"4.2E9\"^^xsd:double      .\n"//
				+ ":right   :floatStyle   \"4.2E9\"^^xsd:float       .\n"//
				+ ":right   :doubleStyle  \"4.2E9\"^^xsd:double      .\n"//
				+ ":right   :floatDouble  \"4.2E9\"^^xsd:float       .\n"//
				+ ":right   :doubleFloat  \"4.2E9\"^^xsd:double      .\n"//
				+ ":right   :floatInteger \"-5\"^^xsd:float          .\n"//
				+ ":right   :integerFloat \"-5\"^^xsd:integer        .\n"//
				+ ":right   :boolean      true                       .\n"//

				+ ":wrong   :string       \"value1\"                 .\n"//
				+ ":wrong   :integer      \"-5\"^^xsd:integer        .\n"//
				+ ":wrong   :decimal      \"-5.0\"^^xsd:decimal      .\n"//
				+ ":wrong   :float        \"4.2E9\"^^xsd:float       .\n"//
				+ ":wrong   :double       \"4.2E9\"^^xsd:double      .\n"//
				+ ":wrong   :floatStyle   \"4.2E9\"^^xsd:float       .\n"//
				+ ":wrong   :doubleStyle  \"4.2E9\"^^xsd:double      .\n"//
				+ ":wrong   :floatDouble  \"4.2E9\"^^xsd:float       .\n"//
				+ ":wrong   :doubleFloat  \"4.2E9\"^^xsd:double      .\n"//
				+ ":wrong   :floatInteger \"-5.1\"^^xsd:float        .\n"//
				+ ":wrong   :integerFloat \"-5\"^^xsd:integer        .\n"//
				+ ":wrong   :boolean      true                       .\n"//

				+ ":type    :string       \"value1\"                 .\n"//
				+ ":type    :integer      \"-5\"^^xsd:integer        .\n"//
				+ ":type    :decimal      \"-5.0\"^^xsd:decimal      .\n"//
				+ ":type    :float        :something                 .\n"//
				+ ":type    :double       :something                 .\n"//
				+ ":type    :floatStyle   :something                 .\n"//
				+ ":type    :doubleStyle  :something                 .\n"//
				+ ":type    :floatDouble  :something                 .\n"//
				+ ":type    :doubleFloat  :something                 .\n"//
				+ ":type    :floatInteger :something                 .\n"//
				+ ":type    :integerFloat :something                 .\n"//
				+ ":type    :boolean      :something                 .\n"//
		).getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/2/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"//

				+ ":right   :string       \"value1\"                 .\n"//
				+ ":right   :integer      \"-5\"^^xsd:integer        .\n"//
				+ ":right   :decimal      \"-5.0\"^^xsd:decimal      .\n"//
				+ ":right   :float        \"4.2E9\"^^xsd:float       .\n"//
				+ ":right   :double       \"4.2E9\"^^xsd:double      .\n"//
				+ ":right   :floatStyle   \"42.0E8\"^^xsd:float      .\n"//
				+ ":right   :doubleStyle  \"42.0E8\"^^xsd:double     .\n"//
				+ ":right   :floatDouble  \"4.2E9\"^^xsd:double      .\n"//
				+ ":right   :doubleFloat  \"4.2E9\"^^xsd:float       .\n"//
				+ ":right   :floatInteger \"-5\"^^xsd:integer        .\n"//
				+ ":right   :integerFloat \"-5\"^^xsd:float          .\n"//
				+ ":right   :boolean      true                       .\n"//

				+ ":wrong   :string       \"value2\"                 .\n"//
				+ ":wrong   :integer      \"-4\"^^xsd:integer        .\n"//
				+ ":wrong   :decimal      \"-4.0\"^^xsd:decimal      .\n"//
				+ ":wrong   :float        \"3.2E9\"^^xsd:float       .\n"//
				+ ":wrong   :double       \"3.2E9\"^^xsd:double      .\n"//
				+ ":wrong   :floatStyle   \"32.0E8\"^^xsd:float      .\n"//
				+ ":wrong   :doubleStyle  \"32.0E8\"^^xsd:double     .\n"//
				+ ":wrong   :floatDouble  \"3.2E9\"^^xsd:double      .\n"//
				+ ":wrong   :doubleFloat  \"3.2E9\"^^xsd:float       .\n"//
				+ ":wrong   :floatInteger \"-5\"^^xsd:integer        .\n"//
				+ ":wrong   :integerFloat \"-5.1\"^^xsd:float        .\n"//
				+ ":wrong   :boolean      false                      .\n"//

				+ ":type    :string       :something                 .\n"//
				+ ":type    :integer      :something                 .\n"//
				+ ":type    :decimal      :something                 .\n"//
				+ ":type    :float        \"4.2E9\"^^xsd:float       .\n"//
				+ ":type    :double       \"4.2E9\"^^xsd:double      .\n"//
				+ ":type    :floatStyle   \"42.0E8\"^^xsd:float      .\n"//
				+ ":type    :doubleStyle  \"42.0E8\"^^xsd:double     .\n"//
				+ ":type    :floatDouble  \"4.2E9\"^^xsd:double      .\n"//
				+ ":type    :doubleFloat  \"4.2E9\"^^xsd:float       .\n"//
				+ ":type    :floatInteger \"-5\"^^xsd:integer        .\n"//
				+ ":type    :integerFloat \"-5\"^^xsd:float          .\n"//
				+ ":type    :boolean      true                       .\n"//
		).getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		SparqlEntityManager.insert(Arrays.asList(//
				new Category("entity", "{"//
						+ "?entity <http://example.org/1/string>       ?string       ."//
						+ "?entity <http://example.org/1/integer>      ?integer      ."//
						+ "?entity <http://example.org/1/decimal>      ?decimal      ."//
						+ "?entity <http://example.org/1/float>        ?float        ."//
						+ "?entity <http://example.org/1/double>       ?double       ."//
						+ "?entity <http://example.org/1/floatStyle>   ?floatStyle   ."//
						+ "?entity <http://example.org/1/doubleStyle>  ?doubleStyle  ."//
						+ "?entity <http://example.org/1/floatDouble>  ?floatDouble  ."//
						+ "?entity <http://example.org/1/doubleFloat>  ?doubleFloat  ."//
						+ "?entity <http://example.org/1/floatInteger> ?floatInteger ."//
						+ "?entity <http://example.org/1/integerFloat> ?integerFloat ."//
						+ "?entity <http://example.org/1/boolean>      ?boolean      ."//
						+ "}"//
						, id1),
				new Category("entity", "{"//
						+ "?entity <http://example.org/2/string>       ?string       ."//
						+ "?entity <http://example.org/2/integer>      ?integer      ."//
						+ "?entity <http://example.org/2/decimal>      ?decimal      ."//
						+ "?entity <http://example.org/2/float>        ?float        ."//
						+ "?entity <http://example.org/2/double>       ?double       ."//
						+ "?entity <http://example.org/2/floatStyle>   ?floatStyle   ."//
						+ "?entity <http://example.org/2/doubleStyle>  ?doubleStyle  ."//
						+ "?entity <http://example.org/2/floatDouble>  ?floatDouble  ."//
						+ "?entity <http://example.org/2/doubleFloat>  ?doubleFloat  ."//
						+ "?entity <http://example.org/2/floatInteger> ?floatInteger ."//
						+ "?entity <http://example.org/2/integerFloat> ?integerFloat ."//
						+ "?entity <http://example.org/2/boolean>      ?boolean      ."//
						+ "}"//
						, id2)),
				metaModel);
		SparqlEntityManager.insert(Arrays.asList(//
				Mapping.of(ResourceFactory.createResource("http://example.org/1/right"),
						ResourceFactory.createResource("http://example.org/2/right")),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/wrong"),
						ResourceFactory.createResource("http://example.org/2/wrong")),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/missing"),
						ResourceFactory.createResource("http://example.org/2/missing"))),
				metaModel);

		// result test
		LiteralDeviationProcessor processor = new LiteralDeviationProcessor();
		LiteralDeviationProcessor.Parameter parameter = new LiteralDeviationProcessor.Parameter();
		parameter.variables = Collections.singletonMap("entity",
				Arrays.asList("string", "integer", "decimal", "double", "float", "floatStyle", "doubleStyle",
						"floatDouble", "doubleFloat", "floatInteger", "integerFloat", "boolean"));
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		Collection<Deviation> deviations = SparqlEntityManager
				.select(new Deviation(null, null, null, null, null, id1, id2, null, null), processor.getResultModel());
		Resource resource1 = ResourceFactory.createResource("http://example.org/1/wrong");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2/wrong");
		assertTrue(deviations
				.contains(new Deviation(null, "entity", "string", resource1, resource2, id1, id2, "value1", "value2")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "integer", resource1, resource2, id1, id2,
				"-5^^http://www.w3.org/2001/XMLSchema#integer", "-4^^http://www.w3.org/2001/XMLSchema#integer")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "decimal", resource1, resource2, id1, id2,
				"-5.0^^http://www.w3.org/2001/XMLSchema#decimal", "-4.0^^http://www.w3.org/2001/XMLSchema#decimal")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "float", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#float", "3.2E9^^http://www.w3.org/2001/XMLSchema#float")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "double", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#double", "3.2E9^^http://www.w3.org/2001/XMLSchema#double")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "floatStyle", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#float", "32.0E8^^http://www.w3.org/2001/XMLSchema#float")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "doubleStyle", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#double", "32.0E8^^http://www.w3.org/2001/XMLSchema#double")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "floatDouble", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#float", "3.2E9^^http://www.w3.org/2001/XMLSchema#double")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "doubleFloat", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#double", "3.2E9^^http://www.w3.org/2001/XMLSchema#float")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "floatInteger", resource1, resource2, id1, id2,
				"-5.1^^http://www.w3.org/2001/XMLSchema#float", "-5^^http://www.w3.org/2001/XMLSchema#integer")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "integerFloat", resource1, resource2, id1, id2,
				"-5^^http://www.w3.org/2001/XMLSchema#integer", "-5.1^^http://www.w3.org/2001/XMLSchema#float")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "boolean", resource1, resource2, id1, id2,
				"true^^http://www.w3.org/2001/XMLSchema#boolean", "false^^http://www.w3.org/2001/XMLSchema#boolean")));
		assertEquals(12, deviations.size());

		Collection<Issue> issues = SparqlEntityManager.select(new Issue(), processor.getResultModel());
		resource1 = ResourceFactory.createResource("http://example.org/1/type");
		resource2 = ResourceFactory.createResource("http://example.org/2/type");
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "string", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "integer", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "decimal", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "float", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "double", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "floatStyle", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "doubleStyle", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "floatDouble", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "doubleFloat", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "floatInteger", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "integerFloat", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "boolean", "literal")));
		assertEquals(12, issues.size());
	}

}
