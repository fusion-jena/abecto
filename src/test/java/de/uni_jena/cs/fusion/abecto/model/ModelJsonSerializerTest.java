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
package de.uni_jena.cs.fusion.abecto.model;

import java.io.ByteArrayOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ModelJsonSerializerTest {

	@Autowired
	ObjectMapper objectMapper;

	/**
	 * Ensure {@link ModelJsonSerializer} is registered and returns unchanged
	 * JSON-LD of {@link org.apache.jena.rdf.model.Model}.
	 */
	@Test
	public void serialize() throws JsonProcessingException {
		Model model = new TestDataGenerator().generateOntology(1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter.create().format(RDFFormat.JSONLD_FLATTEN_PRETTY).source(model).build().output(out);
		String expected = out.toString();
		Assertions.assertEquals(expected, objectMapper.writeValueAsString(model));
	}

}
