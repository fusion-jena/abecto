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

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

public class Default {
	public final static Prologue PROLOGUE = new Prologue();

	static {
		PROLOGUE.setPrefix("owl", OWL2.getURI());
		PROLOGUE.setPrefix("prov", "http://www.w3.org/ns/prov#");
		PROLOGUE.setPrefix("rdf", RDF.getURI());
		PROLOGUE.setPrefix("rdfs", RDFS.getURI());
		PROLOGUE.setPrefix("schema", "http://schema.org/");
		PROLOGUE.setPrefix("skos", SKOS.getURI());
		PROLOGUE.setPrefix("xsd", XSD.getURI());
	}
}
