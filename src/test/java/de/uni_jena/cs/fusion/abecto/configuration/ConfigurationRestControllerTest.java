package de.uni_jena.cs.fusion.abecto.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterEach;
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

import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.processor.WithoutParameters;
import de.uni_jena.cs.fusion.abecto.processor.api.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.UploadSourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.util.ModelUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ConfigurationRestControllerTest {

	@Autowired
	MockMvc mvc;

	private final ResponseBuffer buffer = new ResponseBuffer();

	private final String unknownUuid = UUID.randomUUID().toString();

	private String projectId;
	private String kowledgBaseId;

	@Autowired
	ProjectRepository projectRepository;

	@BeforeEach
	public void init() throws Exception {
		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		projectId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		kowledgBaseId = buffer.getId();
	}

	@AfterEach
	public void cleanup() throws IOException, Exception {
		projectRepository.deleteAll();
	}

	@Test
	public void processingConfiguration() throws Exception {
		// use unknown processor class name
		mvc.perform(MockMvcRequestBuilders.post("/processing").param("class", "Qwert")
				.param("input", new String[] { "550e8400-e29b-11d4-a716-446655440000" })
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		// create new source without parameter
		mvc.perform(MockMvcRequestBuilders.post("/source").param("class", "PathSourceProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		// create new source with parameter
		String parametersJson = "{\"path\":\"/path/to/a/file.owl\"}";
		mvc.perform(MockMvcRequestBuilders.post("/source").param("class", "PathSourceProcessor")
				.param("parameters", parametersJson).param("knowledgebase", kowledgBaseId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// get source
		mvc.perform(MockMvcRequestBuilders.get(String.format("/source/%s", buffer.getId()))
				// TODO remove print
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer).andDo(print());
		JSONAssert.assertEquals(parametersJson, buffer.getJson().path("parameter").path("parameters").toString(),
				false);

		// use unknown knowledgeBase id
		mvc.perform(MockMvcRequestBuilders.post("/source").param("class", "PathSourceProcessor")
				.param("knowledgebase", unknownUuid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		// use unknown processing or source id
		mvc.perform(MockMvcRequestBuilders.post(String.format("/processing/%s/parameter", unknownUuid))
				.param("key", "path").param("value", "some value").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
		mvc.perform(MockMvcRequestBuilders.post(String.format("/source/%s/parameter", unknownUuid)).param("key", "path")
				.param("value", "some value").accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
		mvc.perform(MockMvcRequestBuilders.get(String.format("/processing/%s", unknownUuid))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
		mvc.perform(
				MockMvcRequestBuilders.get(String.format("/source/%s", unknownUuid)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void loadSource() throws Exception {
		// create new source without parameter
		mvc.perform(MockMvcRequestBuilders.post("/source")
				.param("class",
						"de.uni_jena.cs.fusion.abecto.configuration.ConfigurationRestControllerTest$NoUploadProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		mvc.perform(MockMvcRequestBuilders.post(String.format("/source/%s/load", buffer.getId()))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Assertions.assertTrue(NoUploadProcessor.loaded);
	}

	@Test
	public void uploadSourceStream() throws Exception {
		// create new source without parameter
		mvc.perform(MockMvcRequestBuilders.post("/source")
				.param("class",
						"de.uni_jena.cs.fusion.abecto.configuration.ConfigurationRestControllerTest$UploadProcessor")
				.param("knowledgebase", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);

		String content = "File Content";
		MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
		this.mvc.perform(multipart(String.format("/source/%s/load", buffer.getId())).file(multipartFile))
				.andExpect(status().isOk());

		Assertions.assertEquals(content, new String(UploadProcessor.streamData));
		Assertions.assertTrue(NoUploadProcessor.loaded);
	}

	public static class UploadProcessor extends AbstractSourceProcessor<WithoutParameters>
			implements UploadSourceProcessor<WithoutParameters> {

		public static byte[] streamData;
		public static boolean loaded;

		@Override
		public void setUploadStream(InputStream stream) throws IOException {
			streamData = stream.readAllBytes();
		}

		@Override
		protected Model computeResultModel() throws Exception {
			loaded = true;
			return ModelUtils.getEmptyOntModel();
		}
	}

	public static class NoUploadProcessor extends AbstractSourceProcessor<WithoutParameters> {

		public static boolean loaded;

		@Override
		protected Model computeResultModel() throws Exception {
			loaded = true;
			return ModelUtils.getEmptyOntModel();
		}
	}
}