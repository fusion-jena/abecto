package de.uni_jena.cs.fusion.abecto.processor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
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
public class MeasurementTest {
	@Autowired
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