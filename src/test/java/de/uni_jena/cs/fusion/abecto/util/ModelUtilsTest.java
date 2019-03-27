package de.uni_jena.cs.fusion.abecto.util;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ModelUtilsTest {

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(ModelUtils.getEmptyOntModel().listStatements().hasNext());
	}

}
