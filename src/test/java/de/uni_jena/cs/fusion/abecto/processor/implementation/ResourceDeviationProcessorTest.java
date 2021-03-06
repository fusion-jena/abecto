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
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class ResourceDeviationProcessorTest {

	@Test
	void computeResultModel() throws Exception {
		// preparation
		Model model1 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/1/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"

				+ ":right   :other   :wrong                          .\n"//
				+ ":right   :self    :right                          .\n"//

				+ ":wrong   :other   :right                          .\n"//
				+ ":wrong   :self    :wrong                          .\n"//

				+ ":type    :other   :right                          .\n"//
				+ ":type    :self    true                            .\n"//

				+ ":missing :other   :right                          .\n"//
		).getBytes()));
		Model model2 = Models.read(new ByteArrayInputStream((""//
				+ "@prefix  : <http://example.org/2/>                .\n"//
				+ "@prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .\n"

				+ ":right   :other   :wrong                          .\n"//
				+ ":right   :self    :right                          .\n"//

				+ ":wrong   :other   :wrong                          .\n"//
				+ ":wrong   :self    :right                          .\n"//

				+ ":type    :other   true                            .\n"//
				+ ":type    :self    :type                           .\n"//

				+ ":missing :self    :missing                        .\n"//
		).getBytes()));
		Model metaModel = Models.getEmptyOntModel();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		SparqlEntityManager.insert(Arrays.asList(//
				new Category("entity", "{"//
						+ "?entity <http://example.org/1/other> ?other ."//
						+ "?entity <http://example.org/1/self>  ?self  ."//
						+ "}"//
						, id1),
				new Category("entity", "{"//
						+ "?entity <http://example.org/2/other> ?other ."//
						+ "?entity <http://example.org/2/self>  ?self  ."//
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
		ResourceDeviationProcessor processor = new ResourceDeviationProcessor();
		ResourceDeviationProcessor.Parameter parameter = new ResourceDeviationProcessor.Parameter();
		parameter.variables = Collections.singletonMap("entity", Arrays.asList("other", "self"));
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(id1, Collections.singleton(model1), id2, Collections.singleton(model2)));
		processor.addMetaModels(Collections.singleton(metaModel));
		processor.call();

		Collection<Deviation> deviations = SparqlEntityManager
				.select(new Deviation(null, null, null, null, null, id1, id2, null, null), processor.getResultModel());
		assertEquals(2, deviations.size());
		Resource resource1 = ResourceFactory.createResource("http://example.org/1/wrong");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2/wrong");
		assertTrue(deviations.contains(new Deviation(null, "entity", "other", resource1, resource2, id1, id2,
				"<http://example.org/1/right>", "<http://example.org/2/wrong>")));
		assertTrue(deviations.contains(new Deviation(null, "entity", "self", resource1, resource2, id1, id2,
				"<http://example.org/1/wrong>", "<http://example.org/2/right>")));

		Collection<Issue> issues = SparqlEntityManager.select(new Issue(), processor.getResultModel());
		resource1 = ResourceFactory.createResource("http://example.org/1/type");
		resource2 = ResourceFactory.createResource("http://example.org/2/type");
		assertEquals(2, issues.size());
		assertTrue(issues.contains(Issue.unexpectedValueType(id1, resource1, "self", "resource")));
		assertTrue(issues.contains(Issue.unexpectedValueType(id2, resource2, "other", "resource")));
	}
}
