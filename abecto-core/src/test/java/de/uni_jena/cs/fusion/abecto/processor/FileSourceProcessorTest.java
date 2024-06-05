/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

public class FileSourceProcessorTest {
	@Test
	public void computeResultModel() throws Exception {
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		FileSourceProcessor processor = new FileSourceProcessor()//
				.setAssociatedDataset(dataset);
		File source1part1 = new File(
				this.getClass().getResource("../../../../../../tutorial-source1part1.ttl").toURI());
		String source1part1Absolute = source1part1.getAbsolutePath();
		File source1part2 = new File(
				this.getClass().getResource("../../../../../../tutorial-source1part2.ttl").toURI());
		File relativeBasePath = source1part2.getParentFile();
		processor.setRelativeBasePath(relativeBasePath);
		processor.path = Arrays.asList(source1part1Absolute, "tutorial-source1part2.ttl");

		processor.run();

		Model outputPrimaryModel = processor.getOutputPrimaryModel().get();

		assertTrue(outputPrimaryModel.contains(null, null,
				ResourceFactory.createTypedLiteral("45678", XSDDatatype.XSDinteger)));
		assertTrue(outputPrimaryModel.contains(null, null,
				ResourceFactory.createTypedLiteral("67890", XSDDatatype.XSDinteger)));
	}

}
