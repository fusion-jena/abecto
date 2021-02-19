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

import org.apache.jena.rdf.model.Property;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonComponent
public class PropertySerializer extends StdSerializer<Property> {
	private static final long serialVersionUID = 3835469910652060069L;

	public PropertySerializer() {
		this(null);
	}

	public PropertySerializer(Class<Property> t) {
		super(t);
	}

	@Override
	public void serialize(Property value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.toString());
	}
}
