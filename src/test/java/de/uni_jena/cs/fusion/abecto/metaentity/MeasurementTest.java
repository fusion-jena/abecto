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

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MeasurementTest {
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Measurement measurement = new Measurement(ResourceFactory.createResource(), UUID.randomUUID(), "the measure",
				0L, Optional.of("the dimension key 1"), Optional.of("the dimension value 1"),
				Optional.of("the dimension key 2"), Optional.of("the dimension value 2"));
		String serialized = mapper.writeValueAsString(measurement);
		Measurement deserialized = mapper.readValue(serialized, measurement.getClass());
		assertEquals(measurement, deserialized);
	}
}