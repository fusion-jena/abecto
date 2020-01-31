package de.uni_jena.cs.fusion.abecto.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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

import com.fasterxml.jackson.databind.JsonNode;

import de.uni_jena.cs.fusion.abecto.ResponseBuffer;
import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.processor.implementation.JaroWinklerMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.ManualCategoryProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.RdfFileSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.implementation.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReportRestControlerTest {
	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();

	@Autowired
	ProjectRepository projectRepository;

	@AfterEach
	public void cleanup() throws IOException, Exception {
		projectRepository.deleteAll();
	}

	@Test
	void test() throws Exception {
		// TODO use ManualMpiingProcessor to reduce test complexity

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
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("knowledgebase", knowledgBase1Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source1Id = buffer.getId();

		// add source 2
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", RdfFileSourceProcessor.class.getTypeName())
				.param("knowledgebase", knowledgBase2Id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String source2Id = buffer.getId();

		TestDataGenerator testOntologyBuilder = new TestDataGenerator().setClassFactor(5).setObjectPropertyFactor(3)
				.setDataPropertyFactor(3).setIndividualFactor(50).setDensity(4);

		// upload source 1
		MockMultipartFile multipartFileSource1 = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(10).setGapRate(3).stream(1));
		this.mvc.perform(multipart(String.format("/step/%s/load", source1Id)).file(multipartFileSource1))
				.andExpect(status().isOk());

		// upload source 2
		MockMultipartFile multipartFileSource2 = new MockMultipartFile("file",
				testOntologyBuilder.setErrorRate(8).setGapRate(5).stream(2));
		this.mvc.perform(multipart(String.format("/step/%s/load", source2Id)).file(multipartFileSource2))
				.andExpect(status().isOk());

		String transformationParameter = "{\"query\":\"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}\"}";

		// add transformation 1
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", SparqlConstructProcessor.class.getTypeName())
				.param("input", source1Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation1Id = buffer.getId();

		// add transformation 2
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", SparqlConstructProcessor.class.getTypeName())
				.param("input", source2Id).param("parameters", transformationParameter)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String transformation2Id = buffer.getId();

		// add pattern
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", transformation1Id)
				.param("parameters",
						"{\"patterns\":{\"entity\":\"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .\"}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String pattern1Id = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", ManualCategoryProcessor.class.getTypeName())
				.param("input", transformation2Id)
				.param("parameters",
						"{\"patterns\":{\"entity\":\"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .\"}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String pattern2Id = buffer.getId();

		// add mapping
		mvc.perform(MockMvcRequestBuilders.post("/step").param("class", JaroWinklerMappingProcessor.class.getTypeName())
				.param("input", pattern1Id, pattern2Id)
				.param("parameters",
						"{\"threshold\":0.9,\"case_sensitive\":false,\"category\":\"entity\",\"variables\":[\"label\"]}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer).andDo(buffer);
		String mapper = buffer.getId();

		// run project
		mvc.perform(MockMvcRequestBuilders.get(String.format("/project/%s/run", projectId)).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);

		// check processings
		for (JsonNode processingNode : buffer.getJson()) {
			Assertions.assertEquals("SUCCEEDED", processingNode.get("status").asText());
		}

		// get last mapping processing
		mvc.perform(MockMvcRequestBuilders.get(String.format("/step/%s/processing/last", mapper)).param("await", "true")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String mapperProcessing = buffer.getId();

		// get report
		mvc.perform(MockMvcRequestBuilders.get(String.format("/processing/%s/report/MappingReport", mapperProcessing))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(print());

	}

}
