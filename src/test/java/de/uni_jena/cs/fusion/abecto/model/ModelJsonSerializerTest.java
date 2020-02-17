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
