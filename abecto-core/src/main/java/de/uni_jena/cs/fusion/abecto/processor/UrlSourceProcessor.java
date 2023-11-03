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

import java.net.URI;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.util.Models;

public class UrlSourceProcessor extends Processor<UrlSourceProcessor> {

	@Parameter
	public List<Resource> url;

	@Override
	public void run() {
		for (Resource item : url) {
			try {
				Models.read(this.getOutputPrimaryModel().get(), new URI(item.getURI()));
			} catch (Throwable e) {
				throw new RuntimeException(String.format("Failed to read RDF file from URL \"%s\".", item.getURI()), e);
			}
		}
	}
}
