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
import static de.uni_jena.cs.fusion.abecto.TestUtil.containsIssue;
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

class FunctionalMappingProcessorTest {

	@Test
	public void testComputeMapping() throws Exception {
		// prepare aspects
		Query referringPattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property(1) + "> ?value}");
		Query referredPattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property(2) + "> ?value}");
		Aspect referringAspect = new Aspect(aspect(1), "key").setPattern(dataset(1), referringPattern)
				.setPattern(dataset(2), referringPattern);
		Aspect referredAspect = new Aspect(aspect(2), "key").setPattern(dataset(1), referredPattern)
				.setPattern(dataset(2), referredPattern);

		// prepare models
		Literal someLiteral = ResourceFactory.createStringLiteral("some literal");
		Model model1 = ModelFactory.createDefaultModel()//
				.add(resource(111), property(1), resource(141))//
				.add(resource(112), property(1), resource(142))//
				.add(resource(113), property(1), resource(143))//
				.add(resource(121), property(1), someLiteral)//
				.add(resource(122), property(1), someLiteral)//
				.add(resource(123), property(1), someLiteral)//
				.add(resource(131), property(1), resource(151))//
				.add(resource(131), property(1), resource(161))//
				.add(resource(132), property(1), resource(152))//
				.add(resource(132), property(1), resource(162))//
				.add(resource(133), property(1), resource(153))//
				.add(resource(133), property(1), resource(163))//
				.add(resource(141), property(2), "label")//
				.add(resource(142), property(2), "label")//
				.add(resource(143), property(2), "label")//
				.add(resource(151), property(2), "label")//
				.add(resource(152), property(2), "label")//
				.add(resource(153), property(2), "label")//
				.add(resource(161), property(2), "label")//
				.add(resource(162), property(2), "label")//
				.add(resource(163), property(2), "label");
		Model model2 = ModelFactory.createDefaultModel()//
				.add(resource(211), property(1), resource(241))//
				.add(resource(212), property(1), resource(242))//
				.add(resource(213), property(1), resource(243))//
				.add(resource(221), property(1), someLiteral)//
				.add(resource(222), property(1), someLiteral)//
				.add(resource(223), property(1), someLiteral)//
				.add(resource(231), property(1), resource(251))//
				.add(resource(231), property(1), resource(261))//
				.add(resource(232), property(1), resource(252))//
				.add(resource(232), property(1), resource(262))//
				.add(resource(233), property(1), resource(253))//
				.add(resource(233), property(1), resource(263))//
				.add(resource(241), property(2), "label")//
				.add(resource(242), property(2), "label")//
				.add(resource(243), property(2), "label")//
				.add(resource(251), property(2), "label")//
				.add(resource(252), property(2), "label")//
				.add(resource(253), property(2), "label")//
				.add(resource(261), property(2), "label")//
				.add(resource(262), property(2), "label")//
				.add(resource(263), property(2), "label");

		// prepare mapping
		Model mappingModel = ModelFactory.createDefaultModel();
		mappingModel.add(aspect(1), AV.relevantResource, resource(111));
		mappingModel.add(aspect(1), AV.relevantResource, resource(211));
		mappingModel.add(resource(111), AV.correspondsToResource, resource(211));
		mappingModel.add(aspect(1), AV.relevantResource, resource(121));
		mappingModel.add(aspect(1), AV.relevantResource, resource(212));
		mappingModel.add(resource(121), AV.correspondsToResource, resource(212));
		mappingModel.add(aspect(1), AV.relevantResource, resource(131));
		mappingModel.add(aspect(1), AV.relevantResource, resource(213));
		mappingModel.add(resource(131), AV.correspondsToResource, resource(213));
		mappingModel.add(aspect(1), AV.relevantResource, resource(112));
		mappingModel.add(aspect(1), AV.relevantResource, resource(221));
		mappingModel.add(resource(112), AV.correspondsToResource, resource(221));
		mappingModel.add(aspect(1), AV.relevantResource, resource(122));
		mappingModel.add(aspect(1), AV.relevantResource, resource(222));
		mappingModel.add(resource(122), AV.correspondsToResource, resource(222));
		mappingModel.add(aspect(1), AV.relevantResource, resource(132));
		mappingModel.add(aspect(1), AV.relevantResource, resource(223));
		mappingModel.add(resource(132), AV.correspondsToResource, resource(223));
		mappingModel.add(aspect(1), AV.relevantResource, resource(113));
		mappingModel.add(aspect(1), AV.relevantResource, resource(231));
		mappingModel.add(resource(113), AV.correspondsToResource, resource(231));
		mappingModel.add(aspect(1), AV.relevantResource, resource(123));
		mappingModel.add(aspect(1), AV.relevantResource, resource(232));
		mappingModel.add(resource(123), AV.correspondsToResource, resource(232));
		mappingModel.add(aspect(1), AV.relevantResource, resource(133));
		mappingModel.add(aspect(1), AV.relevantResource, resource(233));
		mappingModel.add(resource(133), AV.correspondsToResource, resource(233));

		// run processor
		FunctionalMappingProcessor processor = new FunctionalMappingProcessor().addInputPrimaryModel(dataset(1), model1)
				.addInputPrimaryModel(dataset(2), model2).addInputMetaModel(null, mappingModel)
				.addAspects(referringAspect, referredAspect);
		processor.referringAspect = aspect(1);
		processor.referredAspect = aspect(2);
		processor.referringVariable = "value";
		processor.run();

		// check mappings
		assertTrue(processor.allCorrespondend(resource(141), resource(241)));
		assertTrue(processor.allCorrespondend(resource(151), resource(161), resource(243)));
		assertTrue(processor.allCorrespondend(resource(152), resource(162)));
		assertTrue(processor.allCorrespondend(resource(143), resource(251), resource(261)));
		assertTrue(processor.allCorrespondend(resource(252), resource(262)));
		assertTrue(processor.allCorrespondend(resource(153), resource(163), resource(253), resource(263)));

		// check issues
		Model outputMetaModel1 = processor.getOutputMetaModel(dataset(1));
		assertTrue(containsIssue(resource(121), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel1));
		assertTrue(containsIssue(resource(122), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel1));
		assertTrue(containsIssue(resource(123), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel1));
		Model outputMetaModel2 = processor.getOutputMetaModel(dataset(2));
		assertTrue(containsIssue(resource(221), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel2));
		assertTrue(containsIssue(resource(222), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel2));
		assertTrue(containsIssue(resource(223), "value", someLiteral, aspect(1), "Invalid Value",
				"Should be a resource.", outputMetaModel2));
	}
}
