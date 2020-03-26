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
package de.uni_jena.cs.fusion.abecto.processor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DeviationTest {
	@Autowired
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Deviation deviation = new Deviation(ResourceFactory.createResource(), "entity", "entity",
				ResourceFactory.createResource("http://example.org/a"),
				ResourceFactory.createResource("http://example.org/a"), UUID.randomUUID(), UUID.randomUUID(), "a", "b");
		String serialized = mapper.writeValueAsString(deviation);
		Deviation deserialized = mapper.readValue(serialized, deviation.getClass());
		assertEquals(deviation, deserialized);
	}
}
