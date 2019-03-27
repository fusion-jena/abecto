package de.uni_jena.cs.fusion.abecto.project.knowledgebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

public class KnowledgeBaseTest {

	@Test
	public void testKnowledgeBase() {
		KnowledgeBase kb = new KnowledgeBase();
		assertNull(kb.label);
		assertNull(kb.project);
	}

	@Test
	public void testKnowledgeBaseProjectString() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "label");
		assertEquals(kb.label, "label");
		assertEquals(kb.project, p);
	}

	@Test
	public void testToString() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "label");
		assertTrue(kb.toString().contains("label"));
		assertTrue(kb.toString().contains(kb.getId().toString()));
		assertTrue(kb.toString().contains(p.toString()));
	}

	@Test
	public void testGetProject() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "label");
		assertEquals(kb.getProject(), p);
	}

}
