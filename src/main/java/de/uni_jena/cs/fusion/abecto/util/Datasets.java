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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Abecto;

public class Datasets {

	final static Logger log = LoggerFactory.getLogger(Abecto.class);

	/**
	 * The maximum size of array to allocate.
	 * 
	 * @see {@link BufferedInputStream#MAX_BUFFER_SIZE}
	 */
	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	/**
	 * Supported RDF languages. {@link Lang#NTRIPLES N-Triples} and
	 * {@link Lang#TURTLE Turtle} are implicitly supported due to the support of
	 * {@link Lang#NQUADS N-Quads} and {@link Lang#TRIG TriG}.
	 */
	private static final List<Lang> supportedLanguages = Arrays.asList(Lang.TRIG, Lang.NQUADS, Lang.RDFXML, Lang.N3,
			Lang.JSONLD, Lang.RDFJSON, Lang.RDFTHRIFT, Lang.TRIX, Lang.SHACLC);

	public static void read(Dataset dataset, InputStream in) throws IOException, IllegalArgumentException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(MAX_BUFFER_SIZE);
		// try each known language
		InputStream unclosableIn = new UncloseableInputStream(in);
		LinkedHashMap<Lang, Throwable> throwables = new LinkedHashMap<>();
		for (Lang lang : supportedLanguages) {
			try {
				RDFDataMgr.read(dataset, unclosableIn, lang);
				in.close();
				return;
			} catch (Throwable t) {
				throwables.put(lang, t);
				in.reset();
				continue;
			}
		}
		for (Lang lang : throwables.keySet()) {
			log.info(String.format("Failed to parse %s: %s", lang.getName(), throwables.get(lang).getMessage()
					.replaceFirst("\n\\s+", " ").replaceAll("\n\\s+", ", ").replaceAll("\n", " ")));
		}
		throw new IllegalArgumentException("Unknown RDF language.");
	}
}
