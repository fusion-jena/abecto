package de.uni_jena.cs.fusion.abecto.model;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

@JsonComponent
public class ModelJsonSerializer extends JsonSerializer<Model> {

	@Override
	public void serialize(Model value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeRaw(Models.writeString(value, Lang.JSONLD));
	}
}
