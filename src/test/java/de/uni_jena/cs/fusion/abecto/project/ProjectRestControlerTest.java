package de.uni_jena.cs.fusion.abecto.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProjectRestControlerTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();
	private final String unknownUuid = UUID.randomUUID().toString();

	@Autowired
	ProjectRepository projectRepository;

	@AfterEach
	public void cleanup() throws IOException, Exception {
		projectRepository.deleteAll();
	}

	@Test
	public void test() throws Exception {
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

		// use unknown project id
		mvc.perform(MockMvcRequestBuilders.get("/project/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
		mvc.perform(MockMvcRequestBuilders.delete("/project/" + unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

}
