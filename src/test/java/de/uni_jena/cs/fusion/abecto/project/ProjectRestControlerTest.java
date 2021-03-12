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
package de.uni_jena.cs.fusion.abecto.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;

import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.processor.implementation.JaroWinklerMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.ManualCategoryProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.RdfFileSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.SparqlConstructProcessor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProjectRestControlerTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();
	private final String unknownUuid = UUID.randomUUID().toString();

	@Test
	public void create() throws Exception {
		String projectName = "projectName";
		String projectName2 = "projectName2";

		// create project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", projectName).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name").value(projectName));

		// create or reuse project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", projectName2).param("useIfExists", "true").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name").value(projectName2));

		// try to us same project name
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", projectName).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict());

		// reuse project
		mvc.perform(MockMvcRequestBuilders.post("/project").param("name", projectName).param("useIfExists", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("name").value(projectName));
	}

	@Test
	public void getByName() throws Exception {
		String projectName = "projectName";

		// create project
		String expected = mvc
				.perform(MockMvcRequestBuilders.post("/project").param("name", projectName)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name").value(projectName)).andReturn().getResponse()
				.getContentAsString();

		// try to reuse same project name
		mvc.perform(
				MockMvcRequestBuilders.get("/project").param("name", projectName).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().string(expected));
	}

	@Test
	public void test() throws Exception {
		String projectName = "projectName";

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", projectName).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(projectName, buffer.getJson().path("name").asText());
		String projectId = buffer.getId();

		// return selected project
		mvc.perform(MockMvcRequestBuilders.get("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(projectName, buffer.getJson().path("name").asText());
		assertEquals(projectId, buffer.getId());

		// return not empty project list

		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("name").contains(projectName));
		assertTrue(buffer.getIds().contains(projectId));

		// delete project
		mvc.perform(MockMvcRequestBuilders.delete("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// use unknown project id
		mvc.perform(MockMvcRequestBuilders.get("/project/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
		mvc.perform(MockMvcRequestBuilders.delete("/project/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void run() throws Exception {
		// create project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create ontology 1
		mvc.perform(
				MockMvcRequestBuilders.post("/ontology").param("project", projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String knowledgBase1Id = buffer.getId();

		// create ontology 2
		mvc.perform(
				MockMvcRequestBuilders.post("/ontology").param("project", projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String knowledgBase2Id = buffer.getId();

		// add source 1
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("ontology", knowledgBase1Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source1Id = buffer.getId();

		// add source 2
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("ontology", knowledgBase2Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source2Id = buffer.getId();

		TestDataGenerator testOntologyBuilder = new TestDataGenerator().setClassFactor(5).setObjectPropertyFactor(3)
				.setDataPropertyFactor(3).setIndividualFactor(50).setDensity(4);

		// upload source 1
		MockMultipartFile multipartFileSource1 = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(10).setGapRate(3).stream(1));
		this.mvc.perform(multipart("/node/{source1Id}/load", source1Id).file(multipartFileSource1))
				.andExpect(status().isOk());

		// upload source 2
		MockMultipartFile multipartFileSource2 = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(8).setGapRate(5).stream(2));
		this.mvc.perform(multipart("/node/{source2Id}/load", source2Id).file(multipartFileSource2))
				.andExpect(status().isOk());

		String transformationParameter = "{\"query\":\"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}\"}";

		// add transformation 1
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", SparqlConstructProcessor.class.getTypeName())
				.param("input", source1Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation1Id = buffer.getId();

		// add transformation 2
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", SparqlConstructProcessor.class.getTypeName())
				.param("input", source2Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation2Id = buffer.getId();

		// add categories
		String categoryParameter = "{\"patterns\":{\"entity\":\"{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}\"}}";
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", transformation1Id).param("parameters", categoryParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String category1Id = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", transformation2Id).param("parameters", categoryParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String category2Id = buffer.getId();

		// add mapping
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", JaroWinklerMappingProcessor.class.getTypeName())
				.param("input", category1Id, category2Id)
				.param("parameters",
						"{\"threshold\":0.9,\"case_sensitive\":false,\"category\":\"entity\",\"variables\":[\"label\"]}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// run project
		mvc.perform(MockMvcRequestBuilders.get("/project/{projectId}/run", projectId).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// check processings
		for (JsonNode processingId : buffer.getJson().get("processings")) {
			mvc.perform(MockMvcRequestBuilders.get("/processing/{processingId.asText(}", processingId.asText())
					.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("SUCCEEDED")).andDo(buffer);
		}
	}
}
