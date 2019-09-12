package de.uni_jena.cs.fusion.abecto.model;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Provides a couple of handy methods to easy work with {@link Model}s. 
 */
public class Models {

	public static Model load(InputStream in) throws IOException {
		return load(in, null, null);
	}

	public static Model load(InputStream in, ModelSerializationLanguage lang) throws IOException {
		return load(in, null, lang);
	}

	public static Model load(InputStream in, String base) throws IOException {
		return load(in, base, null);
	}

	public static Model load(InputStream in, String base, ModelSerializationLanguage lang) throws IOException {
		// determine serialization language
		if (Objects.isNull(lang)) {
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
			in.mark(1024);
			lang = ModelSerializationLanguage.determine(new String(in.readNBytes(1024)));
			in.reset();
		}
		// determine base
		if (Objects.isNull(base)) {
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
			in.mark(8192);
			base = lang.determineBase(new String(in.readNBytes(8192)));
			in.reset();
		}
		// Read Model
		Model model = ModelFactory.createDefaultModel();
		model.read(in, base, lang.getApacheJenaKey());
		return model;
	}

	public static OntModel getEmptyOntModel() {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	}

	public static String getStringSerialization(Model model, String lang) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out, lang);
		return out.toString();
	}

	public static byte[] getByteSerialization(Model model, String lang) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out, lang);
		return out.toByteArray();
	}

}
