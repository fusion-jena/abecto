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
package de.uni_jena.cs.fusion.abecto.metaentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.UUID;

import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CategoryTest {
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Category category = new Category("entity", "{?entity a ?entity}", UUID.randomUUID());
		String serialized = mapper.writeValueAsString(category);
		Category deserialized = mapper.readValue(serialized, category.getClass());
		assertEquals(category, deserialized);
	}

	@Test
	public void defaultPrefixes() throws JsonProcessingException {
		new Category("entity", "{?entity owl: ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity prov: ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity rdf:type ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity rdfs:subclassOf ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity schema:isPartOf ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity skos:broader ?entity}", UUID.randomUUID());
		new Category("entity", "{?entity rdfs:label \"String\"^^xsd:string}", UUID.randomUUID());
	}

	@Test
	public void getPatternVariables() {
		Category category = new Category("entity", "{"//
				+ "?entity rdfs:label ?label ;"//
				+ "  rdfs:subClassOf [rdfs:label ?superClassLabel] ."//
				+ "}", UUID.randomUUID());
		Collection<Var> variables = category.getPatternVariables();
		assertTrue(variables.contains(Var.alloc("label")));
		assertTrue(variables.contains(Var.alloc("superClassLabel")));
		assertTrue(variables.contains(Var.alloc("entity")));
		assertFalse(variables.stream().anyMatch((var) -> var.getVarName().startsWith("?")),
				"BlankNodePropertyLists/BlankNodePropertyListPaths helper vars contained");
		assertEquals(3, variables.size());
	}

}
