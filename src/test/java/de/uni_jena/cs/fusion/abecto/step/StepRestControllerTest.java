package de.uni_jena.cs.fusion.abecto.step;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.UploadSourceProcessor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class StepRestControllerTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;

	private final ResponseBuffer buffer = new ResponseBuffer();

	private final String unknownUuid = UUID.randomUUID().toString();

	private String projectId;
	private String knowledgBaseId;

	@BeforeEach
	public void init() throws Exception {
		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		projectId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		knowledgBaseId = buffer.getId();
	}

	@Test
	public void processingStep() throws Exception {
		// use unknown processor class name
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "UnknownProcessor")
				.param("input", new String[] { "550e8400-e29b-11d4-a716-446655440000" })
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		// create new step without parameter
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$ParameterProcessor")
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		// create new step with parameter
		String parametersJson = "{\"parameterName\":\"parameterValue\"}";
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$ParameterProcessor")
				.param("parameters", parametersJson).param("knowledgebase", knowledgBaseId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// get step
		mvc.perform(MockMvcRequestBuilders.get(String.format("/step/%s", buffer.getId()))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		JSONAssert.assertEquals(parametersJson, buffer.getJson().path("parameter").path("parameters").toString(),
				false);

		// use unknown knowledgeBase id
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$ParameterProcessor")
				.param("knowledgebase", unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		// use unknown step id
		mvc.perform(MockMvcRequestBuilders.post(String.format("/step/%s/parameters", unknownUuid))
				.param("key", "parameterName").param("value", "some value").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
		mvc.perform(
				MockMvcRequestBuilders.get(String.format("/step/%s", unknownUuid)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void loadSource() throws Exception {
		// create new source without parameter
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$NoUploadProcessor")
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		mvc.perform(MockMvcRequestBuilders.post(String.format("/step/%s/load", buffer.getId()))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Assertions.assertTrue(NoUploadProcessor.loaded);
	}

	@Test
	public void uploadSourceStream() throws Exception {
		// create new source without parameter
		mvc.perform(MockMvcRequestBuilders.post("/step")
				.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$UploadProcessor")
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		String content = "File Content";
		MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
		this.mvc.perform(multipart(String.format("/step/%s/load", buffer.getId())).file(multipartFile))
				.andExpect(status().isOk());

		Assertions.assertEquals(content, new String(UploadProcessor.streamData));
		Assertions.assertTrue(NoUploadProcessor.loaded);
	}

	@Test
	public void lastProcessing() throws Exception {
		String rdf1 = "<http://example.org/A> <http://example.org/B> <http://example.org/C> .";
		String rdf2 = "<http://example.org/A> <http://example.org/B> <http://example.org/D> .";

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "RdfFileSourceProcessor")
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file", rdf1.getBytes());
		mvc.perform(multipart(String.format("/step/%s/load", sourceId)).file(multipartFileSource))
				.andExpect(status().isOk());

		// upload changed source
		multipartFileSource = new MockMultipartFile("file", rdf2.getBytes());
		mvc.perform(multipart(String.format("/step/%s/load", sourceId)).file(multipartFileSource))
				.andExpect(status().isOk()).andDo(buffer);
		String secondUploadId = buffer.getId();

		// get last processing
		mvc.perform(MockMvcRequestBuilders.get(String.format("/step/%s/processing/last", sourceId))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("id", is(secondUploadId)));
	}

	@Test
	public void list() throws Exception {
		HashSet<String> expected = new HashSet<>();

		for (int i = 0; i < 1; i++) {
			mvc.perform(MockMvcRequestBuilders.post("/step")
					.param("class", "de.uni_jena.cs.fusion.abecto.step.StepRestControllerTest$ParameterProcessor")
					.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk()).andDo(buffer);
			expected.add(buffer.getId());
		}

		mvc.perform(MockMvcRequestBuilders.get("/step").param("project", projectId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		Assertions.assertEquals(expected, new HashSet<>(buffer.getIds()));
	}

	public static class UploadProcessor extends AbstractSourceProcessor<EmptyParameters>
			implements UploadSourceProcessor<EmptyParameters> {

		public static byte[] streamData;
		public static boolean loaded;

		@Override
		public void setUploadStream(InputStream stream) throws IOException {
			streamData = stream.readAllBytes();
		}

		@Override
		protected void computeResultModel() throws Exception {
			loaded = true;
		}
	}

	public static class NoUploadProcessor extends AbstractSourceProcessor<EmptyParameters> {

		public static boolean loaded;

		@Override
		protected void computeResultModel() throws Exception {
			loaded = true;
		}
	}

	public static class ParameterProcessor extends AbstractSourceProcessor<ExampleParameters> {

		public static boolean loaded;

		@Override
		protected void computeResultModel() throws Exception {
			loaded = true;
		}
	}

	@JsonSerialize
	public static class ExampleParameters implements ParameterModel {
		public String parameterName;
	}
}
