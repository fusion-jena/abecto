package de.uni_jena.cs.fusion.abecto.execution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.processor.implementation.ManualCategoryProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.RdfFileSourceProcessor;

public class ExecutionRestControllerTest extends AbstractRepositoryConsumingTest {
	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();

	@Test
	public void getData() throws Exception {
		// create project
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBaseId = buffer.getId();

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file",
				new TestDataGenerator().setClassFactor(1).setIndividualFactor(1).setDensity(4).stream(1));
		this.mvc.perform(multipart(String.format("/step/%s/load", sourceId)).file(multipartFileSource))
				.andExpect(status().isOk());

		// add category
		String categoryParameter = "{\"patterns\":{\"entity\":\"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .\"}}";
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", sourceId).param("parameters", categoryParameter).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		// run project
		mvc.perform(MockMvcRequestBuilders.get(String.format("/project/%s/run", projectId)).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String executionId = buffer.getId();

		// get execution
		mvc.perform(MockMvcRequestBuilders.get(String.format("/execution/%s/data", executionId))
				.param("category", "entity").param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print()).andDo(buffer);

		JSONAssert.assertEquals("{"//
				+ "\"http://example.org/onto1/individual0\":{\"label\":[\"individual0@en\"]},"//
				+ "\"http://example.org/onto1/Class0\":{\"label\":[\"Class0@en\"]}"//
				+ "}", buffer.getString(), JSONCompareMode.LENIENT);
	}

	@Test
	public void getResults() throws Exception {
		// create project
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBaseId = buffer.getId();

		// add source
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("knowledgebase", knowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String sourceId = buffer.getId();

		TestDataGenerator testOntologyBuilder = new TestDataGenerator().setClassFactor(5).setObjectPropertyFactor(3)
				.setDataPropertyFactor(3).setIndividualFactor(50).setDensity(4);

		// upload source
		MockMultipartFile multipartFileSource = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(10).setGapRate(3).stream(1));
		this.mvc.perform(multipart(String.format("/step/%s/load", sourceId)).file(multipartFileSource))
				.andExpect(status().isOk());

		// add category
		String categoryParameter = "{\"patterns\":{\"entity\":\"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .\"}}";
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", sourceId).param("parameters", categoryParameter).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);

		// run project
		mvc.perform(MockMvcRequestBuilders.get(String.format("/project/%s/run", projectId)).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String executionId = buffer.getId();

		// get execution
		mvc.perform(MockMvcRequestBuilders.get(String.format("/execution/%s/results", executionId))
				.param("type", "Category").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		JSONAssert.assertEquals("[{"//
				+ "\"name\":\"entity\","//
				+ "\"pattern\":\"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .\","//
				+ "\"knowledgeBase\":\"" + knowledgBaseId + "\""//
				+ "}]", buffer.getString(), JSONCompareMode.LENIENT);
	}

}
