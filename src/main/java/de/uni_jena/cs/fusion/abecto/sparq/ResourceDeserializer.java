package de.uni_jena.cs.fusion.abecto.sparq;

import java.io.IOException;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@JsonComponent
public class ResourceDeserializer extends StdDeserializer<Resource> {
	private static final long serialVersionUID = 553304485633820927L;

	public ResourceDeserializer() {
		this(null);
	}

	public ResourceDeserializer(Class<Resource> t) {
		super(t);
	}

	@Override
	public Resource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ResourceFactory.createResource(p.getValueAsString());
	}
}
