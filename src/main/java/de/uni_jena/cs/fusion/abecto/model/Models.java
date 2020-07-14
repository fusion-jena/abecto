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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import de.uni_jena.cs.fusion.abecto.util.UncloseableInputStream;

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

	public static Model read(InputStream in) throws IOException, IllegalArgumentException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(MAX_BUFFER_SIZE);
		// try each known language
		InputStream unclosableIn = new UncloseableInputStream(in);
		for (Lang lang : RDFLanguages.getRegisteredLanguages()) {
			try {
				Model model = read(unclosableIn, lang);
				in.close();
				return model;
			} catch (Throwable t) {
				in.reset();
				continue;
			}
		}
		throw new IllegalArgumentException("Unknown RDF language.");
	}

	public static Model read(InputStream in, Lang lang) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, in, lang);
		return model;
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

	public static String writeString(Model model, Lang lang) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Models.write(out, model, lang);
		return out.toString();
	}

	public static OntModel getEmptyOntModel() {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	}

	public static Model getEmptyModel() {
		return ModelFactory.createModelForGraph(Graph.emptyGraph);
	}

	public static OntModel union(Collection<Model> modelCollection, Model... modelArray) {
		OntModel union = getEmptyOntModel();
		modelCollection.forEach(union::addSubModel);
		for (Model model : modelArray) {
			union.addSubModel(model);
		}
		return union;
	}

	public static OntModel union(Model... models) {
		OntModel union = getEmptyOntModel();
		for (Model model : models) {
			union.addSubModel(model);
		}
		return union;
	}

	public static Optional<Resource> readOntologyIri(Model model) {
		Collection<Resource> types = Arrays.asList(OWL2.Ontology, SKOS.ConceptScheme);

		for (Resource type : types) {
			ResIterator iterator = model.listSubjectsWithProperty(RDF.type, type);
			while (iterator.hasNext()) {
				try {
					return Optional.of(iterator.next());
				} catch (Throwable e) {
					// ignore exceptions
				}
			}
		}
		return Optional.empty();
	}

	public static Optional<String> readVersion(Model model) {
		Optional<Resource> ontologyIri = readOntologyIri(model);
		if (ontologyIri.isPresent()) {
			return readVersion(ontologyIri.get(), model);
		}
		return Optional.empty();
	}

	public static Optional<String> readVersion(Resource ontologyIri, Model model) {
		NodeIterator iterator = model.listObjectsOfProperty(ontologyIri, OWL2.versionInfo);
		while (iterator.hasNext()) {
			try {
				RDFNode value = iterator.next();
				if (value.isLiteral()) {
					return Optional.of(value.asLiteral().getLexicalForm());
				}
			} catch (Throwable e) {
				// ignore all exceptions
			}
		}
		return Optional.empty();
	}

	public static Optional<String> readVersionDateTime(Model model) {
		Optional<Resource> ontologyIri = readOntologyIri(model);
		if (ontologyIri.isPresent()) {
			return readVersionDateTime(ontologyIri.get(), model);
		}
		return Optional.empty();
	}

	public static Optional<String> readVersionDateTime(Resource ontologyIri, Model model) {
		Collection<Property> properties = Arrays.asList(DCTerms.modified, DCTerms.available, DCTerms.created,
				DCTerms.date, ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/date"));
		for (Property property : properties) {
			NodeIterator iterator = model.listObjectsOfProperty(ontologyIri, property);
			while (iterator.hasNext()) {
				try {
					RDFNode value = iterator.next();
					if (value.isLiteral()) {
						return Optional.of(value.asLiteral().getLexicalForm());
					}
				} catch (Throwable e) {
					// ignore exceptions
				}
			}
		}
		return Optional.empty();
	}

	public static Optional<Resource> readVersionIri(Model model) {
		NodeIterator iterator = model.listObjectsOfProperty(OWL2.versionIRI);
		while (iterator.hasNext()) {
			try {
				RDFNode value = iterator.next();
				if (value.isResource()) {
					return Optional.of(value.asResource());
				}
			} catch (Throwable e) {
				// ignore all exceptions
			}
		}
		return Optional.empty();
	}

}
