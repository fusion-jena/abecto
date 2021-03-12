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
package de.uni_jena.cs.fusion.abecto.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OntologyRestControllerTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();
	private final String unknownUuid = UUID.randomUUID().toString();

	@Test
	public void test() throws Exception {
		// create project and get project id
		mvc.perform(MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		String ontologyLabel = "ontology label";

		// return empty ontology list
		mvc.perform(MockMvcRequestBuilders.get("/ontology").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty ontology list by project
		mvc.perform(MockMvcRequestBuilders.get("/ontology").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created ontology
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", projectId)
				.param("label", ontologyLabel).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		assertEquals(ontologyLabel, buffer.getJson().path("label").asText());
		String ontologyId = buffer.getId();

		// return selected ontology
		mvc.perform(MockMvcRequestBuilders.get("/ontology/" + ontologyId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(ontologyLabel, buffer.getJson().path("label").asText());
		assertEquals(ontologyId, buffer.getId());

		// return not empty ontology list
		mvc.perform(MockMvcRequestBuilders.get("/ontology").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(ontologyLabel));
		assertTrue(buffer.getJson().findValuesAsText("project").contains(projectId));
		assertTrue(buffer.getIds().contains(ontologyId));

		// return not empty ontology list by project
		mvc.perform(MockMvcRequestBuilders.get("/ontology").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(ontologyLabel));
		assertTrue(buffer.getJson().findValuesAsText("project").contains(projectId));
		assertTrue(buffer.getIds().contains(ontologyId));

		// delete ontology
		mvc.perform(
				MockMvcRequestBuilders.delete("/ontology/" + ontologyId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty ontology list
		mvc.perform(MockMvcRequestBuilders.get("/ontology").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty ontology list by project
		mvc.perform(MockMvcRequestBuilders.get("/ontology").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// use unknown project id
		mvc.perform(MockMvcRequestBuilders.get("/ontology").param("project", unknownUuid)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", unknownUuid).param("label", "")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		// use unknown ontology id
		mvc.perform(MockMvcRequestBuilders.delete("/ontology/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
		mvc.perform(MockMvcRequestBuilders.get("/ontology/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

}
