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
package de.uni_jena.cs.fusion.abecto.metaentity;

import java.util.Objects;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Issue {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Issue")
	public Resource id;
	@SparqlPattern(subject = "id", predicate = "abecto:affectedOntology")
	public final UUID ontology;
	@SparqlPattern(subject = "id", predicate = "abecto:affectedEntity")
	public final Resource entity;
	@SparqlPattern(subject = "id", predicate = "abecto:issueType")
	public final String type;
	@SparqlPattern(subject = "id", predicate = "abecto:issueMessage")
	public final String message;

	public Issue() {
		this.id = null;
		this.ontology = null;
		this.entity = null;
		this.type = null;
		this.message = null;
	}

	public Issue(@Member("id") Resource id, @Member("ontology") UUID ontology,
			@Member("entity") Resource entity, @Member("type") String type, @Member("message") String message) {
		this.id = id;
		this.ontology = ontology;
		this.entity = entity;
		this.type = type;
		this.message = message;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Issue)) {
			return false;
		}
		Issue o = (Issue) other;
		return Objects.equals(this.ontology, o.ontology) && Objects.equals(this.entity, o.entity)
				&& Objects.equals(this.type, o.type) && Objects.equals(this.message, o.message);
	}

	@Override
	public int hashCode() {
		return this.entity.hashCode() + this.type.hashCode() + this.ontology.hashCode() + this.message.hashCode();
	}

	public static Issue unexpectedValueType(UUID ontology, Resource resource, String variableName,
			String expectedType) {
		return new Issue(null, ontology, resource, "UnexpectedValueType",
				String.format("Value of property \"%s\" is not a %s.", variableName, expectedType));
	}

}
