package de.uni_jena.cs.fusion.abecto.rdfModel;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.Entity;
import javax.persistence.Lob;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.util.ModelLoader;
import de.uni_jena.cs.fusion.abecto.util.RdfSerializationLanguage;

@Entity
public class RdfModel extends AbstractEntityWithUUID {

	private final static RdfSerializationLanguage DB_SERIALIZATION_LANG = RdfSerializationLanguage.NTRIPLES;

	@Lob
	private byte[] compressedModel;

	protected RdfModel() {}

	public RdfModel(InputStream in) throws IOException {
		this(in, null, null);
	}

	public RdfModel(InputStream in, RdfSerializationLanguage lang) throws IOException {
		this(in, null, lang);
	}

	public RdfModel(InputStream in, String base) throws IOException {
		this(in, base, null);
	}

	public RdfModel(InputStream in, String base, RdfSerializationLanguage lang) throws IOException {
		consumeModel(ModelLoader.getModel(in, base, lang));
	}

	public RdfModel(Model model) {
		consumeModel(model);
	}

	private void consumeModel(Model model) {
		try {
			// prepare hashing and compression
			ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
			OutputStream out = new GZIPOutputStream(compressedOut);

			// write model to stream
			model.write(out, DB_SERIALIZATION_LANG.getApacheJenaKey(), null);
			out.flush();
			out.close();

			// store compressed graph and hash
			this.compressedModel = compressedOut.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to compress RDF.", e);
		}
	}

	public OntModel getModel() {
		try {
			OntModel model = ModelFactory.createOntologyModel();
			InputStream in = new BufferedInputStream(
					new GZIPInputStream(new ByteArrayInputStream(this.compressedModel)));
			model.read(in, null, DB_SERIALIZATION_LANG.getApacheJenaKey());
			return model;
		} catch (IOException e) {
			throw new RuntimeException("Failed to uncompress RDF.", e);
		}
	}

	@Override
	public String toString() {
		return String.format("RdfModel[id=%s]", this.id);
	}
}
