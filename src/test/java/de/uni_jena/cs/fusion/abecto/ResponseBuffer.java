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
package de.uni_jena.cs.fusion.abecto;

import java.io.IOException;
import java.util.ArrayList;
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
		ArrayList<String> ids = new ArrayList<>();
		JSON.readTree(bytes).elements().forEachRemaining((node) -> ids.add(node.get("id").asText()));
		return ids;
	}
}