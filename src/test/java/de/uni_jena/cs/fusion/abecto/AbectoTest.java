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
