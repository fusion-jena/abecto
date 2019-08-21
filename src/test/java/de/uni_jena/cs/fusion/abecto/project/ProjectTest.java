package de.uni_jena.cs.fusion.abecto.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ProjectTest {

	@Test
	public void testProject() {
		Project p = new Project();
		assertNull(p.label);
	}

	@Test
	public void testProjectString() {
		Project p = new Project("label");
		assertEquals(p.label, "label");
	}

}
