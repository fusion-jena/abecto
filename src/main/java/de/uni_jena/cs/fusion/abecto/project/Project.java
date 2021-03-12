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
package de.uni_jena.cs.fusion.abecto.project;

import javax.persistence.Column;
import javax.persistence.Entity;

import de.uni_jena.cs.fusion.abecto.util.AbstractEntityWithUUID;

@Entity()
public class Project extends AbstractEntityWithUUID {

	@Column(unique = true)
	protected String name;

	protected Project() {
	}

	public Project(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
