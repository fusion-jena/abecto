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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Provides the <a href="https://www.w3.org/TR/vocab-dqv/">Data Quality
 * Vocabulary</a>.
 *
 */
public class DQV {

	/**
	 * The namespace of the vocabulary as a string.
	 */
	public static final String namespace = "http://www.w3.org/ns/dqv#";

	public static final Property computedOn = ResourceFactory.createProperty(namespace, "computedOn");
	public static final Property isMeasurementOf = ResourceFactory.createProperty(namespace, "isMeasurementOf");
	public static final Resource QualityAnnotation = ResourceFactory.createResource(namespace + "QualityAnnotation");
	public static final Property value = ResourceFactory.createProperty(namespace, "value");

	public static String getURI() {
		return namespace;
	}
}
