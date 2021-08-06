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
package de.uni_jena.cs.fusion.abecto.metaentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MappingTest {
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Mapping mapping = new Mapping(ResourceFactory.createResource(), true,
				ResourceFactory.createResource("http://example.org/a"),
				ResourceFactory.createResource("http://example.org/b"));
		String serialized = mapper.writeValueAsString(mapping);
		Mapping deserialized = mapper.readValue(serialized, mapping.getClass());
		assertEquals(mapping, deserialized);
	}
}