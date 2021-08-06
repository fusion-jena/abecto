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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.io.InputStream;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.UploadSourceProcessor;
import de.uni_jena.cs.fusion.abecto.util.Models;

public class RdfFileSourceProcessor extends AbstractSourceProcessor<EmptyParameters>
		implements UploadSourceProcessor<EmptyParameters> {
	
	InputStream stream;

	@Override
	public void computeResultModel() throws Exception {
		this.setModel(Models.read(this.stream));
	}

	@Override
	public void setUploadStream(InputStream stream) {
		this.stream = stream;
	}

}
