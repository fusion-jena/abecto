/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import de.uni_jena.cs.fusion.abecto.AbstractRepositoryConsumingTest;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractSourceProcessor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ParameterRestControllerTest extends AbstractRepositoryConsumingTest {

	@Autowired
	MockMvc mvc;
	private final ResponseBuffer buffer = new ResponseBuffer();

	private final static ObjectMapper JSON = new ObjectMapper();

	@Test
	public void test() throws Exception {
		mvc.perform(
				MockMvcRequestBuilders.post("/project").param("name", "projectName").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(buffer);
		String projectId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/ontology").param("project", projectId).param("name", "ontologyName")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(buffer);
		String kowledgBaseId = buffer.getId();
		mvc.perform(MockMvcRequestBuilders.post("/node")
				.param("class", "de.uni_jena.cs.fusion.abecto.parameter.ParameterRestControllerTest$ParameterProcessor")
				.param("ontology", kowledgBaseId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(buffer);
		String configurationId = buffer.getId();

		// set parameter value
		String parameterValue = "parameterValue";
		mvc.perform(MockMvcRequestBuilders.post("/node/{configurationId}/parameters", configurationId)
				.param("key", "parameterName").param("value", JSON.writeValueAsString(parameterValue))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		// get parameter value
		mvc.perform(MockMvcRequestBuilders.get("/node/{configurationId}/parameters", configurationId)
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
