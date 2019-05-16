package de.uni_jena.cs.fusion.abecto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AbectoRestControllerTest {

	@Autowired
	private MockMvc mvc;

	private ObjectMapper jackson = new ObjectMapper();

	@Test
	public void project() throws Exception {
		String projectLabel = "project label";

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project/list").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created project
		JsonNode projectJson = jackson.readTree(mvc
				.perform(MockMvcRequestBuilders.get("/project/create").param("label", projectLabel)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertEquals(projectLabel, projectJson.path("label").asText());
		UUID uuid = UUID.fromString(projectJson.path("id").asText());

		// return selected project
		projectJson = jackson
				.readTree(mvc.perform(MockMvcRequestBuilders.get("/project/" + uuid).accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertEquals(projectLabel, projectJson.path("label").asText());
		assertEquals(uuid.toString(), projectJson.path("id").asText());

		// return not empty project list
		projectJson = jackson
				.readTree(mvc.perform(MockMvcRequestBuilders.get("/project/list").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertTrue(projectJson.findValuesAsText("label").contains(projectLabel));
		assertTrue(projectJson.findValuesAsText("id").contains(uuid.toString()));

		// delete project
		mvc.perform(MockMvcRequestBuilders.get("/project/delete/" + uuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project/list").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	public void knowledgeBase() throws Exception {
		// create project and get project id
		String projectId = jackson
				.readTree(mvc.perform(MockMvcRequestBuilders.get("/project/create").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
				.path("id").asText();
		String kowledgBaseLabel = "knowledgbase label";

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/list").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/list").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created knowledgeBase
		JsonNode knowledgeBaseJson = jackson.readTree(mvc
				.perform(MockMvcRequestBuilders.get("/knowledgebase/create").param("project", projectId)
						.param("label", kowledgBaseLabel).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print()).andReturn().getResponse().getContentAsByteArray());
		assertEquals(kowledgBaseLabel, knowledgeBaseJson.path("label").asText());
		String knowledgeBaseId = knowledgeBaseJson.path("id").asText();

		// return selected project
		knowledgeBaseJson = jackson.readTree(
				mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertEquals(kowledgBaseLabel, knowledgeBaseJson.path("label").asText());
		assertEquals(knowledgeBaseId, knowledgeBaseJson.path("id").asText());

		// return not empty project list
		knowledgeBaseJson = jackson.readTree(
				mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/list").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertTrue(knowledgeBaseJson.findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(knowledgeBaseJson.findValuesAsText("projectId").contains(projectId));
		assertTrue(knowledgeBaseJson.findValuesAsText("id").contains(knowledgeBaseId));

		// return not empty knowledgeBase list by project
		knowledgeBaseJson = jackson.readTree(mvc
				.perform(MockMvcRequestBuilders.get("/knowledgebase/list").param("project", projectId)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
		assertTrue(knowledgeBaseJson.findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(knowledgeBaseJson.findValuesAsText("projectId").contains(projectId));
		assertTrue(knowledgeBaseJson.findValuesAsText("id").contains(knowledgeBaseId));

		// delete project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/delete/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/list").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/list").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

}
