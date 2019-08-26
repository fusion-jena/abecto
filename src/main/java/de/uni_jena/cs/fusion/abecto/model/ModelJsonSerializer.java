package de.uni_jena.cs.fusion.abecto.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.uni_jena.cs.fusion.abecto.util.RdfSerializationLanguage;

@JsonComponent
public class ModelJsonSerializer extends JsonSerializer<Model> {

	@Override
	public void serialize(Model value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		value.write(out, RdfSerializationLanguage.JSONLD.getApacheJenaKey());
		gen.writeRaw(out.toString());
	}
}
