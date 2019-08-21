package de.uni_jena.cs.fusion.abecto.knowledgebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.project.Project;

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
	public void testGetProject() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "label");
		assertEquals(kb.getProject(), p);
	}

}
