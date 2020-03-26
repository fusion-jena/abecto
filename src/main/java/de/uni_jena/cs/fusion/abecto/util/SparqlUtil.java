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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.*;

public class SparqlUtil {
	private final static Prologue PROLOGUE = new Prologue();

	static {
		PROLOGUE.setPrefix("owl", OWL2.getURI());
		PROLOGUE.setPrefix("prov", "http://www.w3.org/ns/prov#");
		PROLOGUE.setPrefix("rdf", RDF.getURI());
		PROLOGUE.setPrefix("rdfs", RDFS.getURI());
		PROLOGUE.setPrefix("schema", "http://schema.org/");
		PROLOGUE.setPrefix("skos", SKOS.getURI());
		PROLOGUE.setPrefix("xsd", XSD.getURI());
	}
	
	public static Path parsePath(String pattern) throws ParseException {
		InputStream stream = new ByteArrayInputStream(pattern.getBytes());
		SPARQLParser11 parser = new SPARQLParser11(stream);
		parser.setPrologue(PROLOGUE);
		return parser.Path();
	}
	
	public static String pathToString(Path path) {
		return path.toString(PROLOGUE);
	}
	
	public static String sanitizePath(String pattern) throws ParseException {
		return pathToString(parsePath(pattern));
	}

}
