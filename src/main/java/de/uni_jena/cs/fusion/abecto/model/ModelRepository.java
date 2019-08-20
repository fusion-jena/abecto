package de.uni_jena.cs.fusion.abecto.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.util.RdfSerializationLanguage;

@Component
public class ModelRepository {
	private final static Logger log = LoggerFactory.getLogger(ModelRepository.class);

	public final static RdfSerializationLanguage RDF_SERIALIZATION_LANG = RdfSerializationLanguage.NTRIPLES;

	private final File basePath;
	private final Map<String, Model> models = Collections.synchronizedMap(new WeakHashMap<String, Model>());

	public ModelRepository() {
		this.basePath = new File(System.getProperty("user.home") + "/.abecto/models");
		this.basePath.mkdirs();
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

			log.debug(String.format("Write model temporary to \"%s\".", tempFile.getAbsolutePath()));

			try (OutputStream fileOut = new FileOutputStream(tempFile)) {
				try (OutputStream hashOut = new DigestOutputStream(fileOut, digest)) {
					try (OutputStream gzipOut = new GZIPOutputStream(hashOut, true)) {
						// serialize model to file
						serialize(model, gzipOut);
						gzipOut.flush();
					}
				}
			}

			// generate hash based on serialization
			String hash = new BigInteger(1, digest.digest()).toString(16);

			// add model to map
			this.models.put(hash, model);

			// rename file using the hash
			File file = file(hash);

			if (!file.exists()) {
				// model not stored by now
				log.debug(String.format("Move model to \"%s\".", file.getAbsolutePath()));
				tempFile.renameTo(file);
			} else {
				// model already stored
				log.debug(String.format("Model already stored in \"%s\", delete \"%s\".", file.getAbsolutePath(),
						tempFile.getAbsolutePath()));
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
		File folder = new File(basePath, "temp");
		folder.mkdir();
		return new File(folder, UUID.randomUUID().toString() + "." + RDF_SERIALIZATION_LANG.getFileExtension() + ".gz");
	}

	private File file(String hash) {
		File folder = new File(basePath, hash.substring(0, 2));
		folder.mkdir();
		return new File(folder, hash.substring(2) + "." + RDF_SERIALIZATION_LANG.getFileExtension() + ".gz");
	}

	private void serialize(Model model, OutputStream out) throws IOException {
		model.write(out, RDF_SERIALIZATION_LANG.getApacheJenaKey());
	}

	private Model deserialize(InputStream fileIn) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		InputStream gzipIn = new GZIPInputStream(fileIn);
		model.read(gzipIn, null, RDF_SERIALIZATION_LANG.getApacheJenaKey());
		return model;
	}
}
