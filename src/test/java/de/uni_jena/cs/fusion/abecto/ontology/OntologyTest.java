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
		assertNull(o.label);
		assertNull(o.project);
	}

	@Test
	public void testOntologyProjectString() {
		Project p = new Project("");
		Ontology o = new Ontology(p, "label");
		assertEquals(o.label, "label");
		assertEquals(o.project, p);
	}

	@Test
	public void testGetProject() {
		Project p = new Project("");
		Ontology o = new Ontology(p, "label");
		assertEquals(o.getProject(), p);
	}

}
