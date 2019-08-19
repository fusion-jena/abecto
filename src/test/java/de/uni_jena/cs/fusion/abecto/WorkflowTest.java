package de.uni_jena.cs.fusion.abecto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class WorkflowTest {

	@Autowired
	MockMvc mvc;

	private final ResponseBuffer buffer = new ResponseBuffer();

	@Value("classpath:workflowTestOntology1.ttl")
	Resource sourceFile1;
	@Value("classpath:workflowTestOntology2.ttl")
	Resource sourceFile2;

	@Autowired
	ProjectRepository projectRepository;

	@AfterEach
	public void cleanup() throws IOException, Exception {
		projectRepository.deleteAll();
	}

	@Test
	public void workflowTest() throws Exception {
		// create project
		mvc.perform(MockMvcRequestBuilders.post("/project").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();

		// create a KowledgBase 1
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBase1Id = buffer.getId();

		// create a KowledgBase 2
		mvc.perform(MockMvcRequestBuilders.post("/knowledgebase").param("project", projectId)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String knowledgBase2Id = buffer.getId();

		// add source 1
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "RdfFileSourceProcessor")
				.param("knowledgebase", knowledgBase1Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source1Id = buffer.getId();

		// add source 2
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "RdfFileSourceProcessor")
				.param("knowledgebase", knowledgBase2Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source2Id = buffer.getId();

		// upload source 1
		MockMultipartFile multipartFileSource1 = new MockMultipartFile("file", sourceFile1.getInputStream());
		this.mvc.perform(multipart(String.format("/step/%s/load", source1Id)).file(multipartFileSource1))
				.andExpect(status().isOk());

		// upload source 2
		MockMultipartFile multipartFileSource2 = new MockMultipartFile("file", sourceFile2.getInputStream());
		this.mvc.perform(multipart(String.format("/step/%s/load", source2Id)).file(multipartFileSource2))
				.andExpect(status().isOk());

		String transformationParameter = "{\"query\":\"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}\"}";

		// add transformation 1
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "SparqlConstructProcessor")
				.param("input", source1Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation1Id = buffer.getId();

		// add transformation 2
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "SparqlConstructProcessor")
				.param("input", source2Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation2Id = buffer.getId();

		// add mapping
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", "JaroWinklerMappingProcessor")
				.param("input", transformation1Id, transformation2Id)
				.param("parameters", "{\"threshold\":0.9,\"case_sensitive\":false}").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String mappingId = buffer.getId();

		// run project
		mvc.perform(MockMvcRequestBuilders.get(String.format("/project/%s/run", projectId)).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// TODO wait for processing results

		// TODO check generated models

	}

}
