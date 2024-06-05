/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
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
	private final static Map<String, String> DEFAULT_PREFIX_MAP = new HashMap<>();
	private final static PrefixMapping DEFAULT_PREFIX_MAPPING = new PrefixMappingImpl();
	private final static Prologue DEFAULT_PROLOGUE = new Prologue();

	static {
		DEFAULT_PREFIX_MAP.put("av", AV.getURI());
		DEFAULT_PREFIX_MAP.put("dqv", DQV.getURI());
		DEFAULT_PREFIX_MAP.put("oa", OA.getURI());
		DEFAULT_PREFIX_MAP.put("om", OM.getURI());
		DEFAULT_PREFIX_MAP.put("owl", OWL2.getURI());
		DEFAULT_PREFIX_MAP.put("pplan", PPlan.getURI());
		DEFAULT_PREFIX_MAP.put("prov", PROV.getURI());
		DEFAULT_PREFIX_MAP.put("rdf", RDF.getURI());
		DEFAULT_PREFIX_MAP.put("rdfs", RDFS.getURI());
		DEFAULT_PREFIX_MAP.put("schema", SCHEMA.getURI());
		DEFAULT_PREFIX_MAP.put("sdmx-attribute", SdmxAttribute.getURI());
		DEFAULT_PREFIX_MAP.put("skos", SKOS.getURI());
		DEFAULT_PREFIX_MAP.put("xsd", XSD.getURI());

		DEFAULT_PREFIX_MAPPING.setNsPrefixes(DEFAULT_PREFIX_MAP);
		DEFAULT_PREFIX_MAPPING.lock();
		DEFAULT_PROLOGUE.setPrefixMapping(DEFAULT_PREFIX_MAPPING);
	}

	public static Map<String, String> getDefaultPrefixMap() {
		return Collections.unmodifiableMap(DEFAULT_PREFIX_MAP);
	}

	public static PrefixMapping getDefaultPrefixMapping() {
		return DEFAULT_PREFIX_MAPPING;
	}

	public static Prologue getDefaultPrologue() {
		return DEFAULT_PROLOGUE.copy();
	}
}
