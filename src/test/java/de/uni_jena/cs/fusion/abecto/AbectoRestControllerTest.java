package de.uni_jena.cs.fusion.abecto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
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

	private final ResponseBuffer buffer = new ResponseBuffer();

	@After
	public void cleanup() throws IOException, Exception {
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		for (String projectId : buffer.getIds()) {
			mvc.perform(MockMvcRequestBuilders.delete("/project/" + projectId).accept(MediaType.APPLICATION_JSON));
		}
	}

	@Test
	public void project() throws Exception {
		String projectLabel = "project label";

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created project
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("label", projectLabel).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(projectLabel, buffer.getJson().path("label").asText());
		String projectId = buffer.getId();

		// return selected project
		mvc.perform(MockMvcRequestBuilders.get("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(projectLabel, buffer.getJson().path("label").asText());
		assertEquals(projectId, buffer.getId());

		// return not empty project list

		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(projectLabel));
		assertTrue(buffer.getIds().contains(projectId));

		// delete project
		mvc.perform(MockMvcRequestBuilders.delete("/project/" + projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty project list
		mvc.perform(MockMvcRequestBuilders.get("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	private String getNewProject() throws IOException, Exception {
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		return buffer.getId();
	}

	private String getNewKowledgBase() throws IOException, Exception {
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", getNewProject())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		return buffer.getId();
	}

	@Test
	public void knowledgeBase() throws Exception {
		// create project and get project id
		String projectId = getNewProject();

		String kowledgBaseLabel = "knowledgbase label";

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created knowledgeBase
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.param("label", kowledgBaseLabel).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		assertEquals(kowledgBaseLabel, buffer.getJson().path("label").asText());
		String knowledgeBaseId = buffer.getId();

		// return selected project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(kowledgBaseLabel, buffer.getJson().path("label").asText());
		assertEquals(knowledgeBaseId, buffer.getId());

		// return not empty project list

		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(buffer.getJson().findValuesAsText("projectId").contains(projectId));
		assertTrue(buffer.getIds().contains(knowledgeBaseId));

		// return not empty knowledgeBase list by project

		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(kowledgBaseLabel));
		assertTrue(buffer.getJson().findValuesAsText("projectId").contains(projectId));
		assertTrue(buffer.getIds().contains(knowledgeBaseId));

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
		// create a KowledgBase
		String kowledgBaseId = getNewKowledgBase();

		// use invalid processor class name
		mvc.perform(MockMvcRequestBuilders.post("/processing").param("class", "Qwert")
				.param("input", new String[] { "550e8400-e29b-11d4-a716-446655440000" })
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
		// create new source
		mvc.perform(MockMvcRequestBuilders.post("/source").param("class", "PathSourceProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		// set path parameter
		String pathValue = "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl";
		mvc.perform(MockMvcRequestBuilders.post(String.format("/source/%s/parameter", buffer.getId()))
				.param("key", "path").param("value", pathValue).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		// get source
		mvc.perform(MockMvcRequestBuilders.get(String.format("/source/%s", buffer.getId()))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer).andDo(print());
		assertEquals(pathValue, buffer.getJson().path("parameter").path("path").asText());
	}

	private static class ResponseBuffer implements ResultHandler {

		byte[] bytes;

		@Override
		public void handle(MvcResult result) throws Exception {
			this.bytes = result.getResponse().getContentAsByteArray();
		}

		public JsonNode getJson() throws IOException {
			return JACKSON.readTree(bytes);
		}

		public String getId() throws IOException {
			return JACKSON.readTree(bytes).path("id").asText();
		}

		public List<String> getIds() throws IOException {
			return JACKSON.readTree(bytes).findValuesAsText("id");
		}
	}
}
