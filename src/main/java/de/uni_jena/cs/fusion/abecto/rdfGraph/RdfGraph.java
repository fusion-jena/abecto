package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@Entity
public class RdfGraph {

	private final static RdfSerializationLanguage DB_SERIALIZATION_LANG = RdfSerializationLanguage.NTRIPLES;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Lob
	private byte[] compressedGraph;

	protected RdfGraph() {
	}

	public RdfGraph(InputStream in) throws IOException {
		this(in, null, null);
	}

	public RdfGraph(InputStream in, RdfSerializationLanguage lang) throws IOException {
		this(in, null, lang);
	}

	public RdfGraph(InputStream in, String base) throws IOException {
		this(in, base, null);
	}

	public RdfGraph(InputStream in, String base, RdfSerializationLanguage lang) throws IOException {
		// determine serialization language
		if (Objects.isNull(lang)) {
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
			in.mark(1024);
			lang = RdfSerializationLanguage.determine(new String(in.readNBytes(1024)));
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
		consumeModel(model);
	}

	public RdfGraph(Model model) {
		consumeModel(model);
	}

	public RdfGraph(Graph graph) {
		consumeModel(ModelFactory.createModelForGraph(graph));
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
			this.compressedGraph = compressedOut.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to compress RDF.", e);
		}
	}

	public Long getId() {
		return this.id;
	}

	public Graph getGraph() {
		return this.getModel().getGraph();
	}

	public Model getModel() {
		Model model = ModelFactory.createDefaultModel();
		populateModel(model);
		return model;
	}

	public OntModel getOntologyModel() {
		OntModel model = ModelFactory.createOntologyModel();
		populateModel(model);
		return model;
	}

	private void populateModel(Model model) {
		try {
			InputStream in = new BufferedInputStream(
					new GZIPInputStream(new ByteArrayInputStream(this.compressedGraph)));
			model.read(in, null, DB_SERIALIZATION_LANG.getApacheJenaKey());
		} catch (IOException e) {
			throw new RuntimeException("Failed to uncompress RDF.", e);
		}
	}

	@Override
	public String toString() {
		return String.format("RdfGraph[id=%d]", id);
	}
}
