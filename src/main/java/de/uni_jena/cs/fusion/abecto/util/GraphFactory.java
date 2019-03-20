package de.uni_jena.cs.fusion.abecto.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class GraphFactory {

	public static Graph getGraph(InputStream in) throws IOException {
		return getGraph(in, null, null);
	}

	public static Graph getGraph(InputStream in, RdfSerializationLanguage lang) throws IOException {
		return getGraph(in, null, lang);
	}

	public static Graph getGraph(InputStream in, String base) throws IOException {
		return getGraph(in, base, null);
	}

	public static Graph getGraph(InputStream in, String base, RdfSerializationLanguage lang) throws IOException {
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
		return model.getGraph();
	}

}
