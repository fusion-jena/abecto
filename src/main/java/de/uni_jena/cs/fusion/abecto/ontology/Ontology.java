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
package de.uni_jena.cs.fusion.abecto.ontology;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.node.Node;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;
import de.uni_jena.cs.fusion.abecto.util.EntityToIdConverter;

/**
 * Provides a representation of a ontology that can be persisted.
 *
 * Ontologies consist of sources represented by {@link Node}s.
 */
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "project_id", "name" }) })
public class Ontology extends AbstractEntityWithUUID {

	@Column(name = "name")
	protected String name;

	@ManyToOne(optional = false)
	@JsonSerialize(converter = EntityToIdConverter.class)
	protected Project project;

	@OneToMany(mappedBy = "ontology")
	protected Collection<Node> nodes;

	protected Ontology() {
	}

	public Ontology(Project project, String name) {
		this.project = project;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Project getProject() {
		return this.project;
	}

	@JsonIgnore
	public Collection<Node> getNodes() {
		return this.nodes;
	}
}
