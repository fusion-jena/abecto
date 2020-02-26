package de.uni_jena.cs.fusion.abecto.processing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.model.ModelSerializationLanguage;
import de.uni_jena.cs.fusion.abecto.model.Models;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ProcessingRepositoryTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;

	private final ResponseBuffer buffer = new ResponseBuffer();

	private String processingId;
	private Model model;

	@BeforeEach
	public void init() throws Exception {
		model = new TestDataGenerator().generateOntology(1);

		// create project
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBaseId = buffer.getId();// add source
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "RdfFileSourceProcessor")
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		// upload source
		MockMultipartFile multipartFileSource1 = new MockMultipartFile("file",
				Models.getByteSerialization(model, ModelSerializationLanguage.NTRIPLES.getApacheJenaKey()));
		mvc.perform(multipart(String.format("/step/%s/load", sourceId)).file(multipartFileSource1))
				.andExpect(status().isOk());

		// get last processing
		mvc.perform(MockMvcRequestBuilders.get(String.format("/step/%s/processing/last", sourceId))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		processingId = buffer.getId();
	}

	@Test
	public void getModel() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get(String.format("/processing/%s/model", processingId))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"<http://example.org/onto1/individual0> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/onto1/Class0>")));
	}

	@Test
	public void getResult() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get(String.format("/processing/%s/result", processingId))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string(containsString("@graph")));
	}

}
