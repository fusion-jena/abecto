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
public class IssueTest {
	@Autowired
	ObjectMapper mapper;

	@Test
	public void deSerializeation() throws JsonProcessingException {
		Issue issue = new Issue(ResourceFactory.createResource(), UUID.randomUUID(),
				ResourceFactory.createResource("http://example.org/a"), "the type", "the message");
		String serialized = mapper.writeValueAsString(issue);
		Issue deserialized = mapper.readValue(serialized, issue.getClass());
		assertEquals(issue, deserialized);
	}
}