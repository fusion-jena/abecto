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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class TransitiveMappingProcessorTest {
	@Test
	public void computeResultModel() throws Exception {
		Model inputMetaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(Arrays.asList(of(1, 2), of(2, 3), not(3, 4)), inputMetaModel);

		TransitiveMappingProcessor processor = new TransitiveMappingProcessor();
		processor.addMetaModels(Collections.singleton(inputMetaModel));
		Model outputModel = processor.call();

		Collection<Mapping> mappings = SparqlEntityManager.select(Mapping.any(), outputModel);
		assertTrue(mappings.containsAll(Arrays.asList(of(1, 3), not(1, 4), not(2, 4))));
		assertEquals(3, mappings.size());
	}

	private Mapping of(int resourceId1, int resourceId2) {
		return Mapping.of(ResourceFactory.createResource("http://example.org/" + resourceId1),
				ResourceFactory.createResource("http://example.org/" + resourceId2));
	}

	private Mapping not(int resourceId1, int resourceId2) {
		return Mapping.not(ResourceFactory.createResource("http://example.org/" + resourceId1),
				ResourceFactory.createResource("http://example.org/" + resourceId2));
	}
}
