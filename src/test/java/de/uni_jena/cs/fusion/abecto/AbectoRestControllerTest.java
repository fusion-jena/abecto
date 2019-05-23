package de.uni_jena.cs.fusion.abecto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AbectoRestControllerTest {

	@Autowired
	private MockMvc mvc;

	private final static ObjectMapper JACKSON = new ObjectMapper();

	@Test
	public void project() throws Exception {
		String projectLabel = "project label";

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created project
		JsonNode projectJson = getResultJson(mvc.perform(
				MockMvcRequestBuilders.post("/project").param("label", projectLabel).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()));
		assertEquals(projectLabel, projectJson.path("label").asText());
		String projectId = projectJson.path("id").asText();

		// return selected project
		projectJson = getResultJson(
				mvc.perform(MockMvcRequestBuilders.get("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()));
		assertEquals(projectLabel, projectJson.path("label").asText());
		assertEquals(projectId, projectJson.path("id").asText());

		// return not empty project list
		projectJson = getResultJson(
				mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()));
		assertTrue(projectJson.findValuesAsText("label").contains(projectLabel));
		assertTrue(projectJson.findValuesAsText("id").contains(projectId));

		// delete project
		mvc.perform(MockMvcRequestBuilders.delete("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	public void knowledgeBase() throws Exception {
		// create project and get project id
		String projectId = getResultId(
				mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()));
		String kowledgBaseLabel = "knowledgbase label";

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created knowledgeBase
		JsonNode knowledgeBaseJson = getResultJson(mvc
				.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
						.param("label", kowledgBaseLabel).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()));
		assertEquals(kowledgBaseLabel, knowledgeBaseJson.path("label").asText());
		String knowledgeBaseId = knowledgeBaseJson.path("id").asText();

		// return selected project
		knowledgeBaseJson = getResultJson(mvc.perform(
				MockMvcRequestBuilders.get("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()));
		assertEquals(kowledgBaseLabel, knowledgeBaseJson.path("label").asText());
		assertEquals(knowledgeBaseId, knowledgeBaseJson.path("id").asText());

		// return not empty project list
		knowledgeBaseJson = getResultJson(
				mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()));
		assertTrue(knowledgeBaseJson.findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(knowledgeBaseJson.findValuesAsText("projectId").contains(projectId));
		assertTrue(knowledgeBaseJson.findValuesAsText("id").contains(knowledgeBaseId));

		// return not empty knowledgeBase list by project
		knowledgeBaseJson = getResultJson(mvc.perform(MockMvcRequestBuilders.get("/knowledgebase")
				.param("project", projectId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()));
		assertTrue(knowledgeBaseJson.findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(knowledgeBaseJson.findValuesAsText("projectId").contains(projectId));
		assertTrue(knowledgeBaseJson.findValuesAsText("id").contains(knowledgeBaseId));

		// delete project
		mvc.perform(
				MockMvcRequestBuilders.delete("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	public void processingConfiguration() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/processing").param("class", "Qwertz").param("parameters", "")
				.param("input",
						new String[] { "550e8400-e29b-11d4-a716-446655440000", "550e8400-e29b-11d4-a716-446655440001" })
				.accept(MediaType.APPLICATION_JSON)).andDo(print());
	}

	public static JsonNode getResultJson(ResultActions resultActions) throws IOException {
		return JACKSON.readTree(resultActions.andReturn().getResponse().getContentAsByteArray());
	}

	public static String getResultId(ResultActions resultActions) throws IOException {
		return getResultJson(resultActions).path("id").asText();
	}

}
