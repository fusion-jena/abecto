/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelRepository {
	private final static Logger log = LoggerFactory.getLogger(ModelRepository.class);

	public final static Lang SERIALIZATION_LANG = RDFLanguages.NTRIPLES;

	private final File basePath;
	private final Map<String, Model> models = Collections.synchronizedMap(new WeakHashMap<String, Model>());

	public ModelRepository(File basePath) {
		this.basePath = basePath;
		this.basePath.mkdirs();
	}

	public Model get(String hash) {
		return models.computeIfAbsent(hash, this::load);
	}

	private Model load(String hash) {
		try {
			if (hash != null) {
				return deserialize(new FileInputStream(file(hash)));
			} else {
				return null;
			}
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
		return new File(folder, UUID.randomUUID().toString() + "." + fileExtension(SERIALIZATION_LANG) + ".gz");
	}

	private File file(String hash) {
		File folder = new File(basePath, hash.substring(0, 2));
		folder.mkdir();
		return new File(folder, hash.substring(2) + "." + fileExtension(SERIALIZATION_LANG) + ".gz");
	}
	
	private String fileExtension(Lang lang) {
		return lang.getFileExtensions().get(0);
	}

	private void serialize(Model model, OutputStream out) throws IOException {
		Models.write(out, model, SERIALIZATION_LANG);
	}

	private Model deserialize(InputStream fileIn) throws IOException {
		InputStream gzipIn = new GZIPInputStream(fileIn);
		return Models.read(gzipIn, SERIALIZATION_LANG);
	}
}
