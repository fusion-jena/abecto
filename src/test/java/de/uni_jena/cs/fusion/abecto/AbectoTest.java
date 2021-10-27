package de.uni_jena.cs.fusion.abecto;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class AbectoTest {

	@Test
	public void executePlan() throws Throwable {
		File configurationFile = new File(
				this.getClass().getResource("../../../../../tutorial-configuration.trig").toURI());
		File outputFile = Files.createTempFile(null, null).toFile();
		Abecto.executePlan(configurationFile, null, outputFile);
		// TODO check output
	}

}
