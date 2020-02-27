package de.uni_jena.cs.fusion.abecto.sparq;

import java.io.IOException;

import org.apache.jena.rdf.model.Literal;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonComponent
public class LiteralSerializer extends StdSerializer<Literal> {
	private static final long serialVersionUID = 8832538693499963774L;

	public LiteralSerializer() {
		this(null);
	}

	public LiteralSerializer(Class<Literal> t) {
		super(t);
	}

	@Override
	public void serialize(Literal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.getLexicalForm());
	}

}
