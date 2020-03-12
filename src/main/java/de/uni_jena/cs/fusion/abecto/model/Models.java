package de.uni_jena.cs.fusion.abecto.model;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;

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

}
