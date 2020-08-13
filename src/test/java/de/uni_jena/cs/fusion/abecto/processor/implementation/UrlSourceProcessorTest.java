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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.processor.implementation.UrlSourceProcessor.Parameter;

public class UrlSourceProcessorTest {
	@Test
	public void computeResultModel() throws Exception {
		UrlSourceProcessor.Parameter parameter = new Parameter();
		parameter.url = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
		UrlSourceProcessor processor = new UrlSourceProcessor();
		processor.setParameters(parameter);
		Model outputModel = processor.call();
		assertTrue(
				outputModel.contains(ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#first"),
						ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"),
						ResourceFactory.createPlainLiteral("first")));
	}

}
