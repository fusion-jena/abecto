/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.processor;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.util.Models;

public class FileSourceProcessor extends Processor<FileSourceProcessor> {

	final static Logger log = LoggerFactory.getLogger(FileSourceProcessor.class);

	/**
	 * Relative path from the configuration file to the RDF file.
	 */
	@Parameter
	public List<String> path;

	@Override
	public void run() {
		for (String item : path) {
			File file = new File(this.getRelativeBasePath(), item);
			try (FileInputStream in = new FileInputStream(file)) {
				Models.read(this.getOutputPrimaryModel().get(), in);
			} catch (Throwable e) {
				log.error(String.format("Failed to read RDF file \"%s\".\n%s", file, e.getMessage()));
				throw new RuntimeException(String.format("Failed to read RDF file \"%s\".", file), e);
			}
		}
	}

}
