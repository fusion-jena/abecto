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
public class Omission {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Omission")
	public final Resource id;
	/** The category of the missing omittedResource. */
	@SparqlPattern(subject = "id", predicate = "abecto:categoryName")
	public final String categoryName;
	/** The ontology which is missing the omittedResource. */
	@SparqlPattern(subject = "id", predicate = "abecto:ontology")
	public final UUID ontology;
	/** The omittedResource which is missing. */
	@SparqlPattern(subject = "id", predicate = "abecto:omittedResource")
	public final Resource omittedResource;
	/** The ontology which contains the omitted resource. */
	@SparqlPattern(subject = "id", predicate = "abecto:sourceOntology")
	public final UUID source;

	public Omission() {
		this.id = null;
		this.categoryName = null;
		this.ontology = null;
		this.omittedResource = null;
		this.source = null;
	}
	
	public Omission(@Member("id") Resource id, @Member("categoryName") String categoryName,
			@Member("ontology") UUID ontology, @Member("omittedResource") Resource resource, @Member("source") UUID source) {
		this.id = id;
		this.categoryName = categoryName;
		this.ontology = ontology;
		this.omittedResource = resource;
		this.source = source;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Omission)) {
			return false;
		}
		Omission o = (Omission) other;
		return Objects.equals(this.categoryName, o.categoryName) && Objects.equals(this.ontology, o.ontology)
				&& Objects.equals(this.omittedResource, o.omittedResource) && Objects.equals(this.source, o.source);
	}

	@Override
	public int hashCode() {
		return this.categoryName.hashCode() + this.ontology.hashCode() + this.omittedResource.hashCode()
				+ this.source.hashCode();
	}
}
