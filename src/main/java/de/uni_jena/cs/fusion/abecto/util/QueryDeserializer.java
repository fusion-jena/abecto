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
package de.uni_jena.cs.fusion.abecto.util;

import java.io.IOException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class QueryDeserializer extends StdDeserializer<Query> {
	private static final long serialVersionUID = -2170263298467222685L;

	public QueryDeserializer() {
		this(null);
	}

	public QueryDeserializer(Class<Query> t) {
		super(t);
	}

	@Override
	public Query deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return QueryFactory.create(p.getValueAsString());
	}
}