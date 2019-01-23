package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.triples.TriplesPrivate;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdtjena.HDTGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.util.TripelConversionIterator;

@Entity
public class RdfGraph {

	private static final Logger log = LoggerFactory.getLogger(RdfGraph.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(columnDefinition = "BINARY(32) NOT NULL")
	private byte[] hash;
	@Lob
	@Convert(converter = GraphConverter.class)
	private HDTGraph graph;
	@Lob
	private byte[] index;

	protected RdfGraph() {
	}

	@Deprecated
	public RdfGraph(String file) throws IOException, ParserException {
		this(file, null, null);
	}

	@Deprecated
	public RdfGraph(String file, RdfSerializationLanguage lang) throws IOException, ParserException {
		this(file, null, lang);
	}

	@Deprecated
	public RdfGraph(String file, String base) throws IOException, ParserException {
		this(file, base, null);
	}

	@Deprecated
	public RdfGraph(String file, String base, RdfSerializationLanguage lang) throws IOException, ParserException {
		if (Objects.isNull(base) || Objects.isNull(lang)) {
			try (InputStream in = new BufferedInputStream(IOUtil.getFileInputStream(file))) {
				// determine serialization language
				if (Objects.isNull(lang)) {
					in.mark(1024);
					lang = RdfSerializationLanguage.determine(new String(in.readNBytes(1024)));
					in.reset();
				}
				// determine base
				if (Objects.isNull(base)) {
					in.mark(8192);
					base = lang.determineBase(new String(in.readNBytes(8192)));
					in.reset();
				}
			}
		}
		HDT hdt = generateHdt(file, base, lang);
		this.graph = new HDTGraph(hdt);
		this.index = generateIndex(hdt);
		this.hash = generateHash(hdt);
	}

	public RdfGraph(InputStream in) throws IOException, ParserException {
		this(in, null, null);
	}

	public RdfGraph(InputStream in, RdfSerializationLanguage lang) throws IOException, ParserException {
		this(in, null, lang);
	}

	public RdfGraph(InputStream in, String base) throws IOException, ParserException {
		this(in, base, null);
	}

	public RdfGraph(InputStream in, String base, RdfSerializationLanguage lang) throws IOException, ParserException {
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
		model.read(in, base, lang.jenaKey());
		HDT hdt = generateHdt(model.getGraph(), base);
		this.graph = new HDTGraph(hdt);
		this.index = generateIndex(hdt);
		this.hash = generateHash(hdt);
	}

	public RdfGraph(Graph graph, String base) throws IOException, ParserException {
		HDT hdt = generateHdt(graph, base);
		this.index = generateIndex(hdt);
		this.graph = new HDTGraph(hdt);
		this.hash = generateHash(hdt);
	}

	private HDT generateHdt(String path, String base, RdfSerializationLanguage lang)
			throws IOException, ParserException {
		HDT hdt = HDTManager.generateHDT(path, base, lang.hdtKey(), new HDTSpecification(), null);
		removePublicationInformation(hdt);
		return hdt;
	}

	private HDT generateHdt(Graph graph, String base) throws IOException, ParserException {
		HDT hdt = HDTManager.generateHDT(new TripelConversionIterator(graph.find(Triple.ANY)), base,
				new HDTSpecification(), null);
		removePublicationInformation(hdt);
		return hdt;
	}

	private byte[] generateIndex(HDT hdt) throws IOException {
		Triples triples = hdt.getTriples();
		if (triples instanceof TriplesPrivate) {
			TriplesPrivate indexableTriples = (TriplesPrivate) triples;
			indexableTriples.generateIndex(null);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// TODO remove ControlInformation parameter after solving
			// https://github.com/rdfhdt/hdt-java/issues/89
			indexableTriples.saveIndex(out, new ControlInformation(), null);
			return out.toByteArray();
		} else {
			throw new IllegalArgumentException("Provided HDT is not indexable.");
		}

	}

	private byte[] generateHash(HDT hdt) throws IOException {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			hdt.saveToHDT(new DigestOutputStream(OutputStream.nullOutputStream(), messageDigest), null);
			return messageDigest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Hash algorithm not supported.", e);
		}
	}

	public Long getId() {
		return this.id;
	}

	public Model getModel() {
		return ModelFactory.createModelForGraph(this.getGraph());
	}

	public Graph getGraph() {
		Triples triples = this.graph.getHDT().getTriples();
		if (triples instanceof TriplesPrivate) {
			ByteArrayInputStream in = new ByteArrayInputStream(this.index);
			try {
				ControlInformation ci = new ControlInformation();
				ci.load(in);
				((TriplesPrivate) triples).loadIndex(in, ci, null);
			} catch (IOException e) {
				log.info("Failed to load HDT index. Generating new HDT index.", e);
				((TriplesPrivate) triples).generateIndex(null);
			}
		} else {
			throw new IllegalArgumentException("Provided HDT is not indexable.");
		}
		return this.graph;
	}

	public byte[] getHash() {
		return this.hash;
	}

	@Override
	public String toString() {
		return String.format("RdfGraph[id=%d, hash='%064X']", id, new BigInteger(1, this.hash));
	}

	private void removePublicationInformation(HDT hdt) {
		Header header = hdt.getHeader();
		header.remove("_:publicationInformation", "", "");
		header.remove("", "", "_:publicationInformation");
		/*
		 * try (FileOutputStream outputStream = new
		 * FileOutputStream("C:\\Users\\admin\\Documents\\Workspace\\tmp\\temp3.txt")) {
		 * hdt.saveToHDT(outputStream, null); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */
	}
}
