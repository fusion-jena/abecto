package de.uni_jena.cs.fusion.abecto.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;

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
	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ParameterRepository parameterRepository;

	@AfterEach
	public void cleanup() throws Exception {
		processingRepository.deleteAll();
		stepRepository.deleteAll();
		parameterRepository.deleteAll();
		knowledgeBaseRepository.deleteAll();
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
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.parameter.ParameterRestControllerTest$ParameterProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String configurationId = buffer.getId();

		// set parameter value
		String parameterValue = "parameterValue";
		mvc.perform(MockMvcRequestBuilders.post(String.format("/step/%s/parameter", configurationId))
				.param("key", "parameterName").param("value", JSON.writeValueAsString(parameterValue))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		// get parameter value
		mvc.perform(MockMvcRequestBuilders.get(String.format("/step/%s/parameter", configurationId))
				.param("key", "parameterName").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		assertEquals(parameterValue, buffer.getString());
	}

	public static class ParameterProcessor extends AbstractSourceProcessor<ExampleParameters> {

		@Override
		protected void computeResultModel() throws Exception {
			// do nothing
		}
	}

	@JsonSerialize
	public static class ExampleParameters implements ParameterModel {
		public String parameterName;
	}
}
