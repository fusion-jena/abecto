package de.uni_jena.cs.fusion.abecto.RdfModel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.header.Header;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.io.IOUtil;
import org.rdfhdt.hdtjena.HDTGraph;

@Entity
public class RdfModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(columnDefinition = "BINARY(32) NOT NULL")
	private byte[] hash;
	@Lob
	@Convert(converter = ModelConverter.class)
	private Model model;

	protected RdfModel() {
	}

	@Deprecated
	public RdfModel(String file) throws IOException, ParserException {
		this(file, null, null);
	}

	@Deprecated
	public RdfModel(String file, RdfSerializationLanguage lang) throws IOException, ParserException {
		this(file, null, lang);
	}

	@Deprecated
	public RdfModel(String file, String base) throws IOException, ParserException {
		this(file, base, null);
	}

	@Deprecated
	public RdfModel(String file, String base, RdfSerializationLanguage lang) throws IOException, ParserException {
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
		HDT hdt = HDTManager.generateHDT(file, base, lang.hdtKey(), new HDTSpecification(), null);
		removePublicationInformation(hdt);
		HDTGraph hdtGraph = new HDTGraph(hdt);
		this.model = ModelFactory.createModelForGraph(hdtGraph);
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			hdt.saveToHDT(new DigestOutputStream(OutputStream.nullOutputStream(), messageDigest), null);
			this.hash = messageDigest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Hash Algorithm not supported.", e);
		}
	}

	public RdfModel(InputStream in) throws IOException, ParserException {
		this(in, null, null);
	}

	public RdfModel(InputStream in, RdfSerializationLanguage lang) throws IOException, ParserException {
		this(in, null, lang);
	}

	public RdfModel(InputStream in, String base) throws IOException, ParserException {
		this(in, base, null);
	}

	public RdfModel(InputStream in, String base, RdfSerializationLanguage lang) throws IOException, ParserException {
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
		File tempFile = File.createTempFile("abecto-", ".tmp");
		try {
			try (OutputStream out = new FileOutputStream(tempFile)) {
				model.write(out, RdfSerializationLanguage.TURTLE.jenaKey());
			}
			HDT hdt = HDTManager.generateHDT(tempFile.getAbsolutePath(), base, RdfSerializationLanguage.TURTLE.hdtKey(),
					new HDTSpecification(), null);
			removePublicationInformation(hdt);
			HDTGraph hdtGraph = new HDTGraph(hdt);
			this.model = ModelFactory.createModelForGraph(hdtGraph);
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
				hdt.saveToHDT(new DigestOutputStream(OutputStream.nullOutputStream(), messageDigest), null);
				this.hash = messageDigest.digest();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Hash algorithm not supported.", e);
			}
		} finally {
			tempFile.delete();
		}
	}

	public Long getId() {
		return this.id;
	}

	public Model getModel() {
		return this.model;
	}

	public byte[] getHash() {
		return this.hash;
	}

	@Override
	public String toString() {
		return String.format("Customer[id=%d, hash='%064X']", id, new BigInteger(1, this.hash));
	}

	private void removePublicationInformation(HDT hdt) {
		Header header = hdt.getHeader();
		header.remove("_:publicationInformation", "", "");
		header.remove("", "", "_:publicationInformation");
		/*
		try (FileOutputStream outputStream = new FileOutputStream("C:\\Users\\admin\\Documents\\Workspace\\tmp\\temp3.txt")) {
			hdt.saveToHDT(outputStream, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}
}
