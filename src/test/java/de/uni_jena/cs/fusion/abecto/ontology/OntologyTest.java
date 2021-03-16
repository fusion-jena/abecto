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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.project.Project;

public class OntologyTest {

	@Test
	public void testOntology() {
		Ontology o = new Ontology();
		assertNull(o.name);
		assertNull(o.project);
	}

	@Test
	public void testOntologyProjectString() {
		Project p = new Project("");
		Ontology o = new Ontology(p, "name");
		assertEquals(o.name, "name");
		assertEquals(o.project, p);
	}

	@Test
	public void testGetProject() {
		Project p = new Project("");
		Ontology o = new Ontology(p, "name");
		assertEquals(o.getProject(), p);
	}

}
