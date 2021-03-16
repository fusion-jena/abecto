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
package de.uni_jena.cs.fusion.abecto.execution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.processor.implementation.ManualCategoryProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.RdfFileSourceProcessor;

public class ExecutionRestControllerTest extends AbstractRepositoryConsumingTest {
	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();

	@Test
	public void getData() throws Exception {
		// create project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", projectId).param("name", "ontologyName")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBaseId = buffer.getId();

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("ontology", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file",
				new TestDataGenerator().setClassFactor(1).setIndividualFactor(1).setDensity(4).stream(1));
		this.mvc.perform(multipart("/node/{sourceId}/load", sourceId).file(multipartFileSource))
				.andExpect(status().isOk());

		// add category
		String categoryParameter = "{\"patterns\":{\"entity\":\"{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}\"}}";
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", sourceId).param("parameters", categoryParameter).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		// run project
		mvc.perform(MockMvcRequestBuilders.get("/project/{projectId}/run", projectId).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String executionId = buffer.getId();

		// get execution
		mvc.perform(MockMvcRequestBuilders.get("/execution/{executionId}/data", executionId).param("category", "entity")
				.param("ontology", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(print()).andDo(buffer);

		JSONAssert.assertEquals("{"//
				+ "\"http://example.org/onto1/individual0\":{\"label\":[\"individual0@en\"]},"//
				+ "\"http://example.org/onto1/Class0\":{\"label\":[\"Class0@en\"]}"//
				+ "}", buffer.getString(), JSONCompareMode.LENIENT);
	}

	@Test
	public void getResults() throws Exception {
		// create project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", projectId).param("name", "ontologyName")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBaseId = buffer.getId();

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("ontology", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		TestDataGenerator testOntologyBuilder = new TestDataGenerator().setClassFactor(5).setObjectPropertyFactor(3)
				.setDataPropertyFactor(3).setIndividualFactor(50).setDensity(4);

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(10).setGapRate(3).stream(1));
		this.mvc.perform(multipart("/node/{sourceId}/load", sourceId).file(multipartFileSource))
				.andExpect(status().isOk());

		// add category
		String categoryParameter = "{\"patterns\":{\"entity\":\"{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}\"}}";
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", sourceId).param("parameters", categoryParameter).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		// run project
		mvc.perform(MockMvcRequestBuilders.get("/project/{projectId}/run", projectId).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String executionId = buffer.getId();

		// get execution
		mvc.perform(MockMvcRequestBuilders.get("/execution/{executionId}/results", executionId)
				.param("type", "Category").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		JSONAssert.assertEquals("[{"//
				+ "\"name\":\"entity\","//
				+ "\"pattern\":\"{?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .}\","//
				+ "\"ontology\":\"" + knowledgBaseId + "\""//
				+ "}]", buffer.getString(), JSONCompareMode.LENIENT);
	}

	@Test
	public void getMetadata() throws Exception {
		// create project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a Ontology
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", projectId).param("name", "ontologyName")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String ontologyId = buffer.getId();

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/node").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("ontology", ontologyId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file", new ByteArrayInputStream((""//
				+ "<http://example.org/> a                                           <http://www.w3.org/2002/07/owl#Ontology> ;"//
				+ "                      <http://www.w3.org/2002/07/owl#versionIRI>  <http://example.org/2.7.3/> ;"//
				+ "                      <http://purl.org/dc/terms/modified>         \"2020-07-14\" ;"//
				+ "                      <http://www.w3.org/2002/07/owl#versionInfo> \"2.7.3\" .").getBytes()));
		this.mvc.perform(multipart("/node/{sourceId}/load", sourceId).file(multipartFileSource))
				.andExpect(status().isOk());

		// run project
		mvc.perform(MockMvcRequestBuilders.get("/project/{projectId}/run", projectId).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String executionId = buffer.getId();

		// get source loading datetime
		mvc.perform(MockMvcRequestBuilders.get("/node/{sourceId}/processing/last", sourceId).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String loadingDatetime = OffsetDateTime.parse(buffer.getJson().get("startDateTime").asText())
				.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		// get metadata
		mvc.perform(MockMvcRequestBuilders.get("/execution/{executionId}/metadata", executionId)
				.param("category", "entity").param("ontology", ontologyId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		JSONAssert.assertEquals(//
				"{\"" + ontologyId + "\":{\"" + sourceId + "\":{\"loading datetime\":\"" + loadingDatetime
						+ "\",\"parameter\":\"{}\",\"processor\":\"" + RdfFileSourceProcessor.class.getCanonicalName()
						+ "\"}}}",
				buffer.getString(), JSONCompareMode.LENIENT);
	}

}
