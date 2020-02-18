package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.ValueDeviation;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class DeviationProcessorTest {

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
				+ ":right   :self    :right                          .\n"//

				+ ":wrong   :string  \"value1\"                      .\n"//
				+ ":wrong   :integer \"-5\"^^xsd:integer             .\n"//
				+ ":wrong   :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":wrong   :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":wrong   :boolean true                            .\n"//
				+ ":wrong   :self    :wrong                          .\n"//

				+ ":type    :string  \"value1\"                      .\n"//
				+ ":type    :integer \"-5\"^^xsd:integer             .\n"//
				+ ":type    :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":type    :double  :something                      .\n"//
				+ ":type    :boolean :something                      .\n"//
				+ ":type    :self    true                            .\n"//

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
				+ ":right   :self    :right                          .\n"//

				+ ":wrong   :string  \"value2\"                      .\n"//
				+ ":wrong   :integer \"-4\"^^xsd:integer             .\n"//
				+ ":wrong   :decimal \"-4.0\"^^xsd:decimal           .\n"//
				+ ":wrong   :double  \"3.2E9\"^^xsd:double           .\n"//
				+ ":wrong   :boolean false                           .\n"//
				+ ":wrong   :self    :right                          .\n"//

				+ ":type    :string  :something                      .\n"//
				+ ":type    :integer :something                      .\n"//
				+ ":type    :decimal :something                      .\n"//
				+ ":type    :double  \"4.2E9\"^^xsd:double           .\n"//
				+ ":type    :boolean true                            .\n"//
				+ ":type    :self    :type                           .\n"//

				+ ":missing :string  \"value1\"                      .\n"//
				+ ":missing :decimal \"-5.0\"^^xsd:decimal           .\n"//
				+ ":missing   :self  :missing                        .\n"//
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
						+ "?entity <http://example.org/1/self>    ?self    ."//
						, id1),
				new Category("entity", ""//
						+ "?entity <http://example.org/2/string>  ?string  ."//
						+ "?entity <http://example.org/2/integer> ?integer ."//
						+ "?entity <http://example.org/2/decimal> ?decimal ."//
						+ "?entity <http://example.org/2/double>  ?double  ."//
						+ "?entity <http://example.org/2/boolean> ?boolean ."//
						+ "?entity <http://example.org/2/self>    ?self    ."//
						, id2)),
				metaModel);
		SparqlEntityManager.insert(Arrays.asList(//
				Mapping.of(ResourceFactory.createResource("http://example.org/1/right"),
						ResourceFactory.createResource("http://example.org/2/right"), id1, id2, "entity"),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/wrong"),
						ResourceFactory.createResource("http://example.org/2/wrong"), id1, id2, "entity"),
				Mapping.of(ResourceFactory.createResource("http://example.org/1/missing"),
						ResourceFactory.createResource("http://example.org/2/missing"), id1, id2, "entity")),
				metaModel);

		// result test
		DeviationProcessor processor = new DeviationProcessor();
		DeviationProcessor.Parameter parameter = new DeviationProcessor.Parameter();
		parameter.variables = Collections.singletonMap("entity",
				Arrays.asList("string", "integer", "decimal", "double", "boolean", "self"));
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		Collection<ValueDeviation> deviations = SparqlEntityManager.select(
				new ValueDeviation(null, null, null, null, null, id1, id2, null, null), processor.getResultModel());
		assertEquals(6, deviations.size());
		Resource resource1 = ResourceFactory.createResource("http://example.org/1/wrong");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2/wrong");
		assertTrue(deviations.contains(
				new ValueDeviation(null, "entity", "string", resource1, resource2, id1, id2, "value1", "value2")));
		assertTrue(deviations.contains(new ValueDeviation(null, "entity", "integer", resource1, resource2, id1, id2,
				"-5^^http://www.w3.org/2001/XMLSchema#integer", "-4^^http://www.w3.org/2001/XMLSchema#integer")));
		assertTrue(deviations.contains(new ValueDeviation(null, "entity", "decimal", resource1, resource2, id1, id2,
				"-5.0^^http://www.w3.org/2001/XMLSchema#decimal", "-4.0^^http://www.w3.org/2001/XMLSchema#decimal")));
		assertTrue(deviations.contains(new ValueDeviation(null, "entity", "double", resource1, resource2, id1, id2,
				"4.2E9^^http://www.w3.org/2001/XMLSchema#double", "3.2E9^^http://www.w3.org/2001/XMLSchema#double")));
		assertTrue(deviations.contains(new ValueDeviation(null, "entity", "boolean", resource1, resource2, id1, id2,
				"true^^http://www.w3.org/2001/XMLSchema#boolean", "false^^http://www.w3.org/2001/XMLSchema#boolean")));
		assertTrue(deviations.contains(new ValueDeviation(null, "entity", "self", resource1, resource2, id1, id2,
				"<http://example.org/1/wrong>", "<http://example.org/2/right>")));
	}

}
