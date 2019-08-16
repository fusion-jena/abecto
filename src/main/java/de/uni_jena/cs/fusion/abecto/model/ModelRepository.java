package de.uni_jena.cs.fusion.abecto.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.util.RdfSerializationLanguage;

public class ModelRepository {
	private final static RdfSerializationLanguage RDF_SERIALIZATION_LANG = RdfSerializationLanguage.NTRIPLES;

	private final File basePath;
	private final Map<String, Model> models = Collections.synchronizedMap(new WeakHashMap<String, Model>());

	public ModelRepository() {
		this.basePath = new File(System.getProperty("user.home") + "/.abecto/models");
	}

	public Model get(String hash) {
		return models.computeIfAbsent(hash, this::load);
	}

	private Model load(String hash) {
		try {
			return deserialize(new FileInputStream(file(hash)));
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load model from file.", e);
		}
	}

	public String save(Model model) throws IOException {
		File tempFile = tempFile();

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");

			try (OutputStream fileOut = new FileOutputStream(tempFile)) {
				try (OutputStream hashOut = new DigestOutputStream(fileOut, MessageDigest.getInstance("SHA-1"))) {
					try (OutputStream gzipOut = new GZIPOutputStream(hashOut)) {
						// serialize model to file
						serialize(model, hashOut);
					}
				}
			}

			// generate hash based on serialization
			String hash = new String(digest.digest());

			// add model to map
			this.models.put(hash, model);

			// rename file using the hash
			File file = file(hash);

			if (!file.exists()) {
				// model not stored by now
				tempFile.renameTo(file);
			} else {
				// model already stored
				tempFile.delete();
			}

			return hash;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Hash algorithm not supported by JRE.", e);
		}
	}

	public void remove(String hash) {
		models.remove(hash);
		file(hash).delete();
	}

	private File tempFile() {
		return new File(basePath, UUID.randomUUID().toString());
	}

	private File file(String hash) {
		File file = new File(basePath, hash.substring(0, 2) + "/" + hash.substring(2));
		file.getParentFile().mkdirs();
		return file;
	}

	private void serialize(Model model, OutputStream out) throws IOException {
		model.write(out, RDF_SERIALIZATION_LANG.getApacheJenaKey(), null);
		out.flush();
		out.close();
	}

	private Model deserialize(InputStream fileIn) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		InputStream gzipIn = new GZIPInputStream(fileIn);
		model.read(gzipIn, null, RDF_SERIALIZATION_LANG.getApacheJenaKey());
		return model;
	}
}
