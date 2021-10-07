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
package de.uni_jena.cs.fusion.abecto.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import com.google.common.collect.Streams;

/**
 * Provides a couple of handy methods to easy work with {@link Model}s.
 */
public class Models {

	/**
	 * The maximum size of array to allocate.
	 * 
	 * @see {@link BufferedInputStream#MAX_BUFFER_SIZE}
	 */
	private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

	public static OntModel getEmptyOntModel() {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	}

	public static final Collection<Lang> supportedLanguages = Arrays.asList(Lang.RDFXML, Lang.NT, Lang.N3, Lang.TTL,
			Lang.JSONLD, Lang.RDFJSON, Lang.NQ, Lang.TRIG, Lang.RDFTHRIFT, Lang.TRIX, Lang.SHACLC);

	public static Model read(Model model, InputStream in) throws IOException, IllegalArgumentException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(MAX_BUFFER_SIZE);
		// try each supported language
		InputStream unclosableIn = new UncloseableInputStream(in);
		for (Lang lang : supportedLanguages) {
			try {
				RDFDataMgr.read(model, unclosableIn, lang);
				in.close();
				return model;
			} catch (Throwable t) {
				in.reset();
				continue;
			}
		}
		throw new IllegalArgumentException("Unknown RDF language.");
	}

	public static Model read(Model model, URL url) throws IllegalArgumentException, IOException {
		try {
			// using the content type or file extension for language detection
			RDFDataMgr.read(model, url.toString());
			return model;
		} catch (Exception e) {
			// try again using brute force language detection
			return read(model, url.openStream());
		}
	}

	public static OntModel union(Stream<Model> models) {
		OntModel union = getEmptyOntModel();
		models.filter(Objects::nonNull).forEach(union::addSubModel);
		return union;
	}

	public static OntModel union(@Nullable Collection<Model> modelCollection, Model... modelArray) {
		Stream<Model> stream = Arrays.stream(modelArray);
		if (modelCollection != null) {
			stream = Streams.concat(stream, modelCollection.stream());
		}
		return union(stream);
	}

	public static OntModel union(Model... models) {
		return union(Arrays.stream(models));
	}

	public static void write(OutputStream out, Model model, Lang lang) throws IOException {
		if (lang.equals(Lang.JSONLD)) {
			RDFDataMgr.write(out, model, RDFFormat.JSONLD_FLATTEN_PRETTY);
		} else {
			RDFDataMgr.write(out, model, lang);
		}
	}

	public static byte[] writeBytes(Model model, Lang lang) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Models.write(out, model, lang);
		return out.toByteArray();
	}

	public static <T> Optional<T> assertOneOptional(Iterator<T> iterator) throws ToManyElementsException {
		if (iterator.hasNext()) {
			T value = iterator.next();
			if (iterator.hasNext()) {
				throw new ToManyElementsException();
			}
			return Optional.of(value);
		} else {
			return Optional.empty();
		}
	}

	public static <T> T assertOne(Iterator<T> iterator) throws ToManyElementsException, NoSuchElementException {
		if (iterator.hasNext()) {
			T value = iterator.next();
			if (iterator.hasNext()) {
				throw new ToManyElementsException();
			}
			return value;
		} else {
			throw new NoSuchElementException();
		}
	}

}
