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
package de.uni_jena.cs.fusion.abecto;

import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;
import de.uni_jena.cs.fusion.abecto.vocabulary.PROV;
import de.uni_jena.cs.fusion.abecto.vocabulary.SCHEMA;
import de.uni_jena.cs.fusion.abecto.vocabulary.SdmxAttribute;

public class Vocabularies {
	private final static Prologue DEFAULT_PROLOGUE = new Prologue();

	static {
		DEFAULT_PROLOGUE.setPrefix("av", AV.getURI());
		DEFAULT_PROLOGUE.setPrefix("dqv", DQV.getURI());
		DEFAULT_PROLOGUE.setPrefix("oa", OA.getURI());
		DEFAULT_PROLOGUE.setPrefix("om", OM.getURI());
		DEFAULT_PROLOGUE.setPrefix("owl", OWL2.getURI());
		DEFAULT_PROLOGUE.setPrefix("pplan", PPlan.getURI());
		DEFAULT_PROLOGUE.setPrefix("prov", PROV.getURI());
		DEFAULT_PROLOGUE.setPrefix("rdf", RDF.getURI());
		DEFAULT_PROLOGUE.setPrefix("rdfs", RDFS.getURI());
		DEFAULT_PROLOGUE.setPrefix("schema", SCHEMA.getURI());
		DEFAULT_PROLOGUE.setPrefix("sdmx-attribute", SdmxAttribute.getURI());
		DEFAULT_PROLOGUE.setPrefix("skos", SKOS.getURI());
		DEFAULT_PROLOGUE.setPrefix("xsd", XSD.getURI());
	}

	public static Prologue getDefaultPrologue() {
		return DEFAULT_PROLOGUE.copy();
	}
}
