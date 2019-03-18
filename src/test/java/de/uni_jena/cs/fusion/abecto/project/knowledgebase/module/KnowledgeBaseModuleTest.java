package de.uni_jena.cs.fusion.abecto.project.knowledgebase.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;

public class KnowledgeBaseModuleTest {

	@Test
	public void testKnowledgeBaseModule() {
		KnowledgeBaseModule kbm = new KnowledgeBaseModule();
		assertNull(kbm.label);
		assertNull(kbm.knowledgeBase);
	}

	@Test
	public void testKnowledgeBaseModuleKnowledgeBaseString() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "");
		KnowledgeBaseModule kbm = new KnowledgeBaseModule(kb, "label");
		assertEquals(kbm.label, "label");
		assertEquals(kbm.knowledgeBase, kb);
	}

	@Test
	public void testToString() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "");
		KnowledgeBaseModule kbm = new KnowledgeBaseModule(kb, "label");
		assertTrue(kbm.toString().contains("label"));
		assertTrue(kbm.toString().contains(kbm.getId().toString()));
		assertTrue(kbm.toString().contains(kb.toString()));
	}

	@Test
	public void testGetKnowledgeBase() {
		Project p = new Project("");
		KnowledgeBase kb = new KnowledgeBase(p, "");
		KnowledgeBaseModule kbm = new KnowledgeBaseModule(kb, "label");
		assertEquals(kbm.getKnowledgeBase(), kb);
	}

}
