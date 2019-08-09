package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ParameterRestControllerTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();

	private final static ObjectMapper JSON = new ObjectMapper();

	@Autowired
	ProjectRepository projectRepository;

	@AfterEach
	public void cleanup() throws IOException, Exception {
		projectRepository.deleteAll();
	}

	@Test
	public void test() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String kowledgBaseId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/source").param("class", "PathSourceProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String configurationId = buffer.getId();

		// set path parameter
		String pathValue = "/path/to/a/file.owl";
		mvc.perform(
				MockMvcRequestBuilders.post(String.format("/source/%s/parameter", configurationId)).param("key", "path")
						.param("value", JSON.writeValueAsString(pathValue)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// get parameter value
		mvc.perform(MockMvcRequestBuilders.get(String.format("/source/%s/parameter", configurationId))
				.param("key", "path").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer)
				.andDo(print());
		assertEquals(pathValue, buffer.getString());

	}
}
