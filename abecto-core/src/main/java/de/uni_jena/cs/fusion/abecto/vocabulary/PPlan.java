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
package de.uni_jena.cs.fusion.abecto.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the
 * <a href="http://vocab.linkeddata.es/p-plan/index.html">P-Plan</a> vocabulary.
 *
 */
public class PPlan {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://purl.org/net/p-plan#";

	public final static Property isStepOfPlan = ResourceFactory.createProperty(namespace, "isStepOfPlan");
	public final static Property isPrecededBy = ResourceFactory.createProperty(namespace, "isPrecededBy");
	public final static Property correspondsToStep = ResourceFactory.createProperty(namespace, "correspondsToStep");

	public static String getURI() {
		return namespace;
	}
}
