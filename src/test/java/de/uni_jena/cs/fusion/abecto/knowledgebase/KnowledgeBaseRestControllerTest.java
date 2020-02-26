package de.uni_jena.cs.fusion.abecto.knowledgebase;

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
public class KnowledgeBaseRestControllerTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();
	private final String unknownUuid = UUID.randomUUID().toString();

	@Test
	public void test() throws Exception {
		// create project and get project id
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		String knowledgBaseLabel = "knowledgbase label";

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// return created knowledgeBase
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.param("label", knowledgBaseLabel).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		assertEquals(knowledgBaseLabel, buffer.getJson().path("label").asText());
		String knowledgeBaseId = buffer.getId();

		// return selected knowledgeBase
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertEquals(knowledgBaseLabel, buffer.getJson().path("label").asText());
		assertEquals(knowledgeBaseId, buffer.getId());

		// return not empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(knowledgBaseLabel));
		assertTrue(buffer.getJson().findValuesAsText("project").contains(projectId));
		assertTrue(buffer.getIds().contains(knowledgeBaseId));

		// return not empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		assertTrue(buffer.getJson().findValuesAsText("label").contains(knowledgBaseLabel));
		assertTrue(buffer.getJson().findValuesAsText("project").contains(projectId));
		assertTrue(buffer.getIds().contains(knowledgeBaseId));

		// delete knowledgeBase
		mvc.perform(
				MockMvcRequestBuilders.delete("/knowledgebase/" + knowledgeBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		// return empty knowledgeBase list
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("[]"));

		// return empty knowledgeBase list by project
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().json("[]"));

		// use unknown project id
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase").param("project", unknownUuid)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", unknownUuid).param("label", "")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		// use unknown knowledgeBase id
		mvc.perform(MockMvcRequestBuilders.delete("/knowledgebase/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
		mvc.perform(MockMvcRequestBuilders.get("/knowledgebase/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

}
