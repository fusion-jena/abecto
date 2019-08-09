package de.uni_jena.cs.fusion.abecto;

import java.io.IOException;
import java.util.List;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseBuffer implements ResultHandler {

	byte[] bytes;

	private final static ObjectMapper JSON = new ObjectMapper();

	@Override
	public void handle(MvcResult result) throws Exception {
		this.bytes = result.getResponse().getContentAsByteArray();
	}

	public JsonNode getJson() throws IOException {
		return JSON.readTree(bytes);
	}

	public String getString() throws IOException {
		return new String(bytes);
	}

	public String getId() throws IOException {
		return JSON.readTree(bytes).path("id").asText();
	}

	public List<String> getIds() throws IOException {
		return JSON.readTree(bytes).findValuesAsText("id");
	}
}