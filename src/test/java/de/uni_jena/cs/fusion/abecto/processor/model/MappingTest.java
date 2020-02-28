package de.uni_jena.cs.fusion.abecto.processor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

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
public class MappingTest {
	@Autowired
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Mapping mapping = new Mapping(ResourceFactory.createResource(), true,
				ResourceFactory.createResource("http://example.org/a"),
				ResourceFactory.createResource("http://example.org/b"), Optional.of("the category"));
		String serialized = mapper.writeValueAsString(mapping);
		Mapping deserialized = mapper.readValue(serialized, mapping.getClass());
		assertEquals(mapping, deserialized);
	}
}