package de.uni_jena.cs.fusion.abecto.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

public class ModelsTest {

	@Test
	public void testGetEmptyOntModel() {
		assertFalse(Models.getEmptyOntModel().listStatements().hasNext());
	}

	@Test
	public void loadVeryShortOntologies() throws Exception {
		Models.read(new ByteArrayInputStream(("@prefix : <http://example.org/>.\n:s :p :o.").getBytes()));
	}

}
