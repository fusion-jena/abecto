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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class UsePresentMappingProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		Model inputPrimaryModel = ModelFactory.createDefaultModel();

		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");

		Property sameAs = ResourceFactory.createProperty("http://example.org/sameAs");
		Property same = ResourceFactory.createProperty("http://example.org/same");
		Property as = ResourceFactory.createProperty("http://example.org/as");

		Resource type = ResourceFactory.createResource("http://eample.org/type");

		Resource a1 = inputPrimaryModel.createResource("http://example.org/a1", type);
		Resource a2 = inputPrimaryModel.createResource("http://example.org/a2", type);
		Resource b1 = inputPrimaryModel.createResource("http://example.org/b1", type);
		Resource bLink = inputPrimaryModel.createResource("http://example.org/b-link", type);
		Resource b2 = inputPrimaryModel.createResource("http://example.org/b2", type);
		Resource c1 = inputPrimaryModel.createResource("http://example.org/c1", type);
		Resource c2 = inputPrimaryModel.createResource("http://example.org/c2", type);
		Resource d1 = inputPrimaryModel.createResource("http://example.org/d1", type);
		Resource d2 = inputPrimaryModel.createResource("http://example.org/d2", type);
		Resource e1 = inputPrimaryModel.createResource("http://example.org/e1", type);
		Literal issueLiteral = inputPrimaryModel.createLiteral("issueLiteral");

		inputPrimaryModel.add(a1, sameAs, a2).add(b1, same, bLink).add(bLink, as, b2).add(c1, sameAs, c2)
				.add(d1, sameAs, d2).add(e1, sameAs, issueLiteral);

		Model inputMetaModel = ModelFactory.createDefaultModel();
		inputMetaModel.add(c1, AV.affectedAspect, aspectIri);
		inputMetaModel.add(c2, AV.affectedAspect, aspectIri);
		inputMetaModel.add(d1, AV.affectedAspect, aspectIri);
		inputMetaModel.add(d2, AV.affectedAspect, aspectIri);
		inputMetaModel.add(c1, AV.correspondsToResource, c2);
		inputMetaModel.add(d1, AV.correspondsNotToResource, d2);

		Aspect aspect = new Aspect(aspectIri, "key");
		aspect.setPattern(dataset, QueryFactory.create("SELECT ?key WHERE {?key a <" + type.getURI() + ">}"));

		UsePresentMappingProcessor processor = new UsePresentMappingProcessor();
		processor.addAspects(aspect);
		processor.aspect = aspectIri;
		processor.assignmentPaths = Arrays.asList("<http://example.org/sameAs>",
				"<http://example.org/same>/<http://example.org/as>");
		processor.addInputMetaModel(null, inputMetaModel);
		processor.addInputPrimaryModel(dataset, inputPrimaryModel);
		processor.run();

		assertTrue(processor.allCorrespondend(a1, a2));
		assertTrue(processor.allCorrespondend(b1, b2));
		assertTrue(processor.allCorrespondend(c1, c2));
		assertFalse(processor.allCorrespondend(d1, d2));

		Model outputMetaModel = processor.getOutputMetaModel(null);
		// assert issue present
		Query query = QueryFactory.create(""//
				+ "ASK WHERE {"//
				+ "  ?issue a <" + AV.Issue + "> ;"//
				+ "         <" + AV.affectedAspect + "> <" + aspectIri + "> ;"//
				+ "         <" + AV.affectedValue + "> \"" + issueLiteral + "\" ;"//
				+ "         <" + AV.issueType + "> \"Invalid Value\" ;"//
				+ "}");
		assertTrue(QueryExecutionFactory.create(query, outputMetaModel).execAsk());
	}
}
