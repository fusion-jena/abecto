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
package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the <a href="https://www.w3.org/TR/prov-overview/">PROV-O</a>
 * vocabulary.
 *
 */
public class PROV {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://www.w3.org/ns/prov#";

	public final static Property startedAtTime = ResourceFactory.createProperty(namespace, "startedAtTime");
	public final static Property endedAtTime = ResourceFactory.createProperty(namespace, "endedAtTime");
	public final static Property wasGeneratedBy = ResourceFactory.createProperty(namespace, "wasGeneratedBy");
	public final static Property used = ResourceFactory.createProperty(namespace, "used");

	public static String getURI() {
		return namespace;
	}
}
