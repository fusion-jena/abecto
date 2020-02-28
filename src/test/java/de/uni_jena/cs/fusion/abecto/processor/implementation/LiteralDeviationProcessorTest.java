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
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Deviation;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class LiteralDeviationProcessorTest {

	@Test
	void computeResultModel() throws Exception {
		// preparation
		Model model1 = Models.load(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/1/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"

				+ ":right   :string  \"value1\"                      .\n"//
				+ ":right   :integer \"-5\"^^xsd:integer             .\n"//
				+ ":right   :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":right   :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":right   :boolean true                            .\n"//

				+ ":wrong   :string  \"value1\"                      .\n"//
				+ ":wrong   :integer \"-5\"^^xsd:integer             .\n"//
				+ ":wrong   :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":wrong   :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":wrong   :boolean true                            .\n"//

				+ ":type    :string  \"value1\"                      .\n"//
				+ ":type    :integer \"-5\"^^xsd:integer             .\n"//
				+ ":type    :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":type    :double  :something                      .\n"//
				+ ":type    :boolean :something                      .\n"//

				+ ":missing :integer \"-5\"^^xsd:integer             .\n"//
				+ ":missing :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":missing :boolean true                            .\n"//
		).getBytes()));
		Model model2 = Models.load(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/2/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"

				+ ":right   :string  \"value1\"                      .\n"//
				+ ":right   :integer \"-5\"^^xsd:integer             .\n"//
				+ ":right   :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":right   :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":right   :boolean true                            .\n"//

				+ ":wrong   :string  \"value2\"                      .\n"//
				+ ":wrong   :integer \"-4\"^^xsd:integer             .\n"//
				+ ":wrong   :decimal \"-4.0\"^^xsd:decimal           .\n"//
				+ ":wrong   :double  \"3.2E9\"^^xsd:double           .\n"//
				+ ":wrong   :boolean false                           .\n"//

				+ ":type    :string  :something                      .\n"//
				+ ":type    :integer :something                      .\n"//
				+ ":type    :decimal :something                      .\n"//
				+ ":type    :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":type    :boolean true                            .\n"//

				+ ":missing :string  \"value1\"                      .\n"//
				+ ":missing :decimal \"-5.0\"^^xsd:decimal           .\n"//
		).getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		SparqlEntityManager.insert(Arrays.asList(//
				new Category("entity", ""//
						+ "?entity <http://example.org/1/string>  ?string  ."//
						+ "?entity <http://example.org/1/integer> ?integer ."//
						+ "?entity <http://example.org/1/decimal> ?decimal ."//
						+ "?entity <http://example.org/1/double>  ?double  ."//
						+ "?entity <http://example.org/1/boolean> ?boolean ."//
						, id1),
				new Category("entity", ""//
						+ "?entity <http://example.org/2/string>  ?string  ."//
						+ "?entity <http://example.org/2/integer> ?integer ."//
						+ "?entity <http://example.org/2/decimal> ?decimal ."//
						+ "?entity <http://example.org/2/double>  ?double  ."//
						+ "?entity <http://example.org/2/boolean> ?boolean ."//
						, id2)),
				metaModel);
		SparqlEntityManager.insert(Arrays.asList(//
				Mapping.of(ResourceFactory.createResource("http://example.org/1/right"),
						ResourceFactory.createResource("http://example.org/2/right"), "entity"),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/wrong"),
						ResourceFactory.createResource("http://example.org/2/wrong"), "entity"),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/missing"),
						ResourceFactory.createResource("http://example.org/2/missing"), "entity")),
				metaModel);

		// result test
		LiteralDeviationProcessor processor = new LiteralDeviationProcessor();
		LiteralDeviationProcessor.Parameter parameter = new LiteralDeviationProcessor.Parameter();
		parameter.variables = Collections.singletonMap("entity",
				Arrays.asList("string", "integer", "decimal", "double", "boolean"));
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		Collection<Deviation> deviations = SparqlEntityManager
				.select(new Deviation(null, null, null, null, null, id1, id2, null, null), processor.getResultModel());
		assertEquals(5, deviations.size());
		Resource resource1 = ResourceFactory.createResource("http://example.org/1/wrong");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2/wrong");
		assertTrue(deviations
				.contains(new Deviation(null, "entity", "string", resource1, resource2, id1, id2, "value1", "value2")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "integer", resource1, resource2, id1, id2,
				"-5^^http://www.w3.org/2001/XMLSchema#integer", "-4^^http://www.w3.org/2001/XMLSchema#integer")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "decimal", resource1, resource2, id1, id2,
				"-5.0^^http://www.w3.org/2001/XMLSchema#decimal", "-4.0^^http://www.w3.org/2001/XMLSchema#decimal")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "double", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#double", "3.2E9^^http://www.w3.org/2001/XMLSchema#double")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "boolean", resource1, resource2, id1, id2,
				"true^^http://www.w3.org/2001/XMLSchema#boolean", "false^^http://www.w3.org/2001/XMLSchema#boolean")));

		Collection<Issue> issues = SparqlEntityManager.select(new Issue(), processor.getResultModel());
		resource1 = ResourceFactory.createResource("http://example.org/1/type");
		resource2 = ResourceFactory.createResource("http://example.org/2/type");
		assertEquals(5, issues.size());
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "string", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "integer", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "decimal", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "double", "literal")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "boolean", "literal")));
	}

}
