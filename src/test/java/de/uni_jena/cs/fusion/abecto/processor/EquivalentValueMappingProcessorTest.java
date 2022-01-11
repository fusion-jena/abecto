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
import static de.uni_jena.cs.fusion.abecto.TestUtil.dataset;
import static de.uni_jena.cs.fusion.abecto.TestUtil.property;
import static de.uni_jena.cs.fusion.abecto.TestUtil.resource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

class EquivalentValueMappingProcessorTest {

	@Test
	public void testComputeMapping() throws Exception {
		// prepare aspects
		Query aspectPattern = QueryFactory.create("SELECT ?key ?variable1 ?variable2 ?variable3 WHERE {?key <" + property(1) + "> ?variable1; <"
				+ property(2) + "> ?variable2; <" + property(3) + "> ?variable3}");
		Aspect aspect = new Aspect(aspect(1), "key").setPattern(dataset(1), aspectPattern).setPattern(dataset(2),
				aspectPattern);

		// prepare models
		Model model1 = ModelFactory.createDefaultModel();
		Model model2 = ModelFactory.createDefaultModel();
		Model mappingModel = ModelFactory.createDefaultModel();

		Literal someLiteral = ResourceFactory.createStringLiteral("some literal");
		Literal otherLiteral = ResourceFactory.createStringLiteral("other literal");

		// add some data
		model1.add(resource("case01-dataset1"), property(1), someLiteral);
		model1.add(resource("case01-dataset1"), property(2), someLiteral);
		model1.add(resource("case01-dataset1"), property(3), someLiteral);
		model2.add(resource("case01-dataset2"), property(1), otherLiteral);
		model2.add(resource("case01-dataset2"), property(2), otherLiteral);
		model2.add(resource("case01-dataset2"), property(3), otherLiteral);

		model1.add(resource("case02-dataset1"), property(1), someLiteral);
		model1.add(resource("case02-dataset1"), property(2), someLiteral);
		model1.add(resource("case02-dataset1"), property(3), someLiteral);
		model2.add(resource("case02-dataset2"), property(1), someLiteral);
		model2.add(resource("case02-dataset2"), property(2), otherLiteral);
		model2.add(resource("case02-dataset2"), property(3), otherLiteral);

		model1.add(resource("case03-dataset1"), property(1), someLiteral);
		model1.add(resource("case03-dataset1"), property(2), someLiteral);
		model1.add(resource("case03-dataset1"), property(3), someLiteral);
		model2.add(resource("case03-dataset2"), property(1), someLiteral);
		model2.add(resource("case03-dataset2"), property(2), someLiteral);
		model2.add(resource("case03-dataset2"), property(3), otherLiteral);

		model1.add(resource("case04-dataset1"), property(1), someLiteral);
		model1.add(resource("case04-dataset1"), property(2), someLiteral);
		model1.add(resource("case04-dataset1"), property(3), someLiteral);
		model2.add(resource("case04-dataset2"), property(1), someLiteral);
		model2.add(resource("case04-dataset2"), property(2), someLiteral);
		model2.add(resource("case04-dataset2"), property(3), someLiteral);

		model1.add(resource("case05-dataset1"), property(1), resource("some"));
		model1.add(resource("case05-dataset1"), property(2), resource("some"));
		model1.add(resource("case05-dataset1"), property(3), resource("some"));
		model2.add(resource("case05-dataset2"), property(1), resource("other"));
		model2.add(resource("case05-dataset2"), property(2), resource("other"));
		model2.add(resource("case05-dataset2"), property(3), resource("other"));

		model1.add(resource("case06-dataset1"), property(1), resource("some"));
		model1.add(resource("case06-dataset1"), property(2), resource("some"));
		model1.add(resource("case06-dataset1"), property(3), resource("some"));
		model2.add(resource("case06-dataset2"), property(1), resource("some"));
		model2.add(resource("case06-dataset2"), property(2), resource("other"));
		model2.add(resource("case06-dataset2"), property(3), resource("other"));

		model1.add(resource("case07-dataset1"), property(1), resource("some"));
		model1.add(resource("case07-dataset1"), property(2), resource("some"));
		model1.add(resource("case07-dataset1"), property(3), resource("some"));
		model2.add(resource("case07-dataset2"), property(1), resource("some"));
		model2.add(resource("case07-dataset2"), property(2), resource("some"));
		model2.add(resource("case07-dataset2"), property(3), resource("other"));

		model1.add(resource("case08-dataset1"), property(1), resource("some"));
		model1.add(resource("case08-dataset1"), property(2), resource("some"));
		model1.add(resource("case08-dataset1"), property(3), resource("some"));
		model2.add(resource("case08-dataset2"), property(1), resource("some"));
		model2.add(resource("case08-dataset2"), property(2), resource("some"));
		model2.add(resource("case08-dataset2"), property(3), resource("some"));
		
		model1.add(resource("case09-dataset1"), property(1), resource("some"));
		model1.add(resource("case09-dataset1"), property(2), resource("some"));
		model1.add(resource("case09-dataset1"), property(3), resource("some"));
		model2.add(resource("case09-dataset2"), property(1), resource("other"));
		model2.add(resource("case09-dataset2"), property(2), resource("other"));
		model2.add(resource("case09-dataset2"), property(3), resource("other"));

		model1.add(resource("case10-dataset1"), property(1), resource("some"));
		model1.add(resource("case10-dataset1"), property(2), resource("some"));
		model1.add(resource("case10-dataset1"), property(3), resource("some"));
		model2.add(resource("case10-dataset2"), property(1), resource("equivalent"));
		model2.add(resource("case10-dataset2"), property(2), resource("other"));
		model2.add(resource("case10-dataset2"), property(3), resource("other"));

		model1.add(resource("case11-dataset1"), property(1), resource("some"));
		model1.add(resource("case11-dataset1"), property(2), resource("some"));
		model1.add(resource("case11-dataset1"), property(3), resource("some"));
		model2.add(resource("case11-dataset2"), property(1), resource("equivalent"));
		model2.add(resource("case11-dataset2"), property(2), resource("equivalent"));
		model2.add(resource("case11-dataset2"), property(3), resource("other"));

		model1.add(resource("case12-dataset1"), property(1), resource("some"));
		model1.add(resource("case12-dataset1"), property(2), resource("some"));
		model1.add(resource("case12-dataset1"), property(3), resource("some"));
		model2.add(resource("case12-dataset2"), property(1), resource("equivalent"));
		model2.add(resource("case12-dataset2"), property(2), resource("equivalent"));
		model2.add(resource("case12-dataset2"), property(3), resource("equivalent"));

		model1.add(resource("case13-dataset1"), property(1), someLiteral);
		model1.add(resource("case13-dataset1"), property(2), someLiteral);
		model1.add(resource("case13-dataset1"), property(3), someLiteral);
		model2.add(resource("case13-dataset2"), property(1), otherLiteral);
		model2.add(resource("case13-dataset2"), property(1), someLiteral);
		model2.add(resource("case13-dataset2"), property(2), otherLiteral);
		model2.add(resource("case13-dataset2"), property(2), someLiteral);
		model2.add(resource("case13-dataset2"), property(3), otherLiteral);
		model2.add(resource("case13-dataset2"), property(3), someLiteral);

		mappingModel.add(resource("some"), AV.correspondsToResource, resource("equivalent"));

		EquivalentValueMappingProcessor processor;

		// run processor
		processor = new EquivalentValueMappingProcessor().addInputPrimaryModel(dataset(1), model1)
				.addInputPrimaryModel(dataset(2), model2).addInputMetaModel(null, mappingModel).addAspects(aspect);
		processor.aspect = aspect.getIri();
		processor.variables = Arrays.asList("variable1", "variable2", "variable3");
		processor.run();

		// check results
		assertFalse(processor.allCorrespondend(resource("case01-dataset1"), resource("case01-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case02-dataset1"), resource("case02-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case03-dataset1"), resource("case03-dataset2")));
		assertTrue( processor.allCorrespondend(resource("case04-dataset1"), resource("case04-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case05-dataset1"), resource("case05-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case06-dataset1"), resource("case06-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case07-dataset1"), resource("case07-dataset2")));
		assertTrue( processor.allCorrespondend(resource("case08-dataset1"), resource("case08-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case09-dataset1"), resource("case09-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case00-dataset1"), resource("case00-dataset2")));
		assertFalse(processor.allCorrespondend(resource("case11-dataset1"), resource("case11-dataset2")));
		assertTrue( processor.allCorrespondend(resource("case12-dataset1"), resource("case12-dataset2")));
		assertTrue( processor.allCorrespondend(resource("case13-dataset1"), resource("case13-dataset2")));
	}
}
