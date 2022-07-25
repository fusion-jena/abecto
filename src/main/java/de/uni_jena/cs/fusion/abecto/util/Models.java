/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;

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

	public static final Collection<Lang> supportedLanguages = Arrays.asList(Lang.RDFXML, Lang.TRIG, Lang.NQ,
			Lang.JSONLD, Lang.RDFJSON, Lang.RDFTHRIFT, Lang.TRIX, Lang.SHACLC);

	public static Model read(Model model, InputStream in) throws IOException, IllegalArgumentException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(MAX_BUFFER_SIZE);
		// try each supported language
		InputStream unclosableIn = new UncloseableInputStream(in);
		LinkedHashMap<Lang, Throwable> throwables = new LinkedHashMap<>();
		for (Lang lang : supportedLanguages) {
			try {
				RDFParser.source(unclosableIn).lang(lang).errorHandler(ErrorHandlerFactory.errorHandlerNoLogging)
						.parse(model);
				in.close();
				return model;
			} catch (Throwable t) {
				throwables.put(lang, t);
				in.reset();
				continue;
			}
		}
		throw new IllegalArgumentException(
				"Unknown RDF language.\\n  "
						+ throwables
								.entrySet().stream().map(e -> String.format("Failed to parse %s: %s",
										e.getKey().getName(), e.getValue().getMessage().replaceFirst("\n\\s+", " ")
												.replaceAll("\n\\s+", ", ").replaceAll("\n", " ")))
								.collect(Collectors.joining("\n  ")));
	}

	public static Model read(Model model, URI uri) throws IllegalArgumentException, IOException, InterruptedException {
		try {
			// using the content type or file extension for language detection
			RDFParser.source(uri.toString()).errorHandler(ErrorHandlerFactory.errorHandlerNoLogging).parse(model);
			return model;
		} catch (Exception e) {
			// try again using brute force language detection

			// create a client
			var client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

			// create a request
			var request = HttpRequest.newBuilder(uri)
					.header("accept",
							supportedLanguages.stream().map(Lang::getHeaderString).collect(Collectors.joining(", "))
									+ ", */*;q=0.8")
					.build();

			return read(model, client.send(request, BodyHandlers.ofInputStream()).body());
		}
	}

	public static Model union(Model baseModel, Stream<Model> models) {
		MultiUnion unionGraph = new MultiUnion();
		if (baseModel != null) {
			unionGraph.addGraph(baseModel.getGraph());
			unionGraph.setBaseGraph(baseModel.getGraph());
		}
		models.filter(Objects::nonNull).map(Model::getGraph).forEach(unionGraph::addGraph);
		return ModelFactory.createModelForGraph(unionGraph);
	}

	public static Model union(Stream<Model> models) {
		return union(null, models);
	}

	public static Model union(@Nullable Collection<Model> modelCollection) {
		return union(null, modelCollection);
	}

	public static Model union(Model baseModel, @Nullable Collection<Model> modelCollection) {
		Stream<Model> stream;
		if (modelCollection != null) {
			stream = modelCollection.stream();
		} else {
			stream = Stream.empty();
		}
		return union(baseModel, stream);
	}

	public static Model union(Model baseModel, Model... models) {
		return union(baseModel, Arrays.stream(models));
	}

	public static void write(OutputStream out, Model model, Lang lang) throws IOException {
		if (lang.equals(Lang.JSONLD)) {
			RDFDataMgr.write(out, model, RDFFormat.JSONLD_PRETTY);
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
