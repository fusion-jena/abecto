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

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Mapping {
	public static Mapping not() {
		return new Mapping(null, false, null, null);
	}

	public static Mapping not(Resource resource1, Resource resource2) {
		return new Mapping(null, false, resource1, resource2);
	}

	public static Mapping of() {
		return new Mapping(null, true, null, null);
	}

	public static Mapping of(Resource resource1, Resource resource2) {
		return new Mapping(null, true, resource1, resource2);
	}

	/**
	 * @return prototype without any restrictions
	 */
	public static Mapping any() {
		return new Mapping();
	}

	@SparqlPattern(predicate = "rdf:type", object = "abecto:Mapping")
	public Resource id;

	@SparqlPattern(subject = "id", predicate = "abecto:resourcesMap")
	public final Boolean resourcesMap;

	@SparqlPattern(subject = "id", predicate = "abecto:mappedResource1")
	public final Resource resource1;

	@SparqlPattern(subject = "id", predicate = "abecto:mappedResource2")
	public final Resource resource2;

	public Mapping() {
		this(null, null, null, null);
	}

	public Mapping(@Member("id") Resource id, @Member("resourcesMap") Boolean entitiesMap,
			@Member("resource1") Resource resource1, @Member("resource2") Resource resource2) {
		this.id = id;
		this.resourcesMap = entitiesMap;
		if (resource1 == null || resource2 != null && resource1.getURI().compareTo(resource2.getURI()) < 0) {
			this.resource1 = resource1;
			this.resource2 = resource2;
		} else {
			this.resource1 = resource2;
			this.resource2 = resource1;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Mapping))
			return false;
		Mapping other = (Mapping) o;
		return this.resourcesMap.equals(other.resourcesMap) && this.resource1.equals(other.resource1)
				&& this.resource2.equals(other.resource2);
	}

	@Override
	public int hashCode() {
		return this.resource1.getURI().hashCode() + this.resource2.getURI().hashCode() + (this.resourcesMap ? 1 : 0);
	}

	public Mapping inverse() {
		return new Mapping(null, !this.resourcesMap, this.resource1, this.resource2);
	}
}
