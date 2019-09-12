package de.uni_jena.cs.fusion.abecto.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class ModelUtilsTest {

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(Models.getEmptyOntModel().listStatements().hasNext());
	}

}
