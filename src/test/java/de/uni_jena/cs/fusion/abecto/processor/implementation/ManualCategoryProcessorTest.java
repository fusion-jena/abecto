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

import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;

public class ManualCategoryProcessorTest {

	private static RdfFileSourceProcessor source;

	@BeforeAll
	public static void init() throws Exception {
		source = new RdfFileSourceProcessor();
		source.setUploadStream(new TestDataGenerator().stream(1));
		source.setOntology(UUID.randomUUID());
		source.call();
	}

	@Test
	public void computeResultModel() throws Exception {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put("good", "{?good <" + RDFS.subClassOf + "> <" + OWL.Thing + ">}");
		parameter.patterns.put("bad", "{?bad <" + RDF.type + "> <" + RDFS.Class + ">}");
		processor.setParameters(parameter);
		processor.computeResultModel();
		Model model = processor.getResultModel();
		Assertions
				.assertEquals(
						2, model
								.listResourcesWithProperty(RDF.type,
										ResourceFactory.createResource(
												"http://fusion.cs.uni-jena.de/ontology/abecto#Category"))
								.toSet().size());

	}

	@Test
	public void invalidPattern() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put("test", "{}");
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void notParsablePattern() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put("test", "");
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalStateException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyPatternList() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		processor.setParameters(parameter);
		Model model = processor.getResultModel();
		Assertions
				.assertEquals(
						0, model
								.listResourcesWithProperty(RDF.type,
										ResourceFactory.createResource(
												"http://fusion.cs.uni-jena.de/ontology/abecto#Category"))
								.toSet().size());
	}
}
