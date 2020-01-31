package de.uni_jena.cs.fusion.abecto.sparq;

import java.io.IOException;

import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ResourceSerializer extends StdSerializer<Resource> {
	private static final long serialVersionUID = 8832538693499963774L;

	public ResourceSerializer() {
		this(null);
	}

	public ResourceSerializer(Class<Resource> t) {
		super(t);
	}

	@Override
	public void serialize(Resource value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.getURI());
	}

}
