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
package de.uni_jena.cs.fusion.abecto.processor;

import java.util.logging.Level;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import openllet.jena.PelletReasoner;
import openllet.shared.tools.Log;

public class OpenlletReasoningProcessor extends Processor<OpenlletReasoningProcessor> {

	@Override
	public void run() {
		// suppress progress logging
		Log.setLevel(Level.OFF); // TODO test
		
		Model inputPrimaryModel = this.getInputPrimaryModelUnion(this.getAssociatedDataset().get());
		
		// prepare reasoning
		InfModel inferenceModel = ModelFactory.createInfModel(new PelletReasoner(), inputPrimaryModel);
		
		// execute and process result
		this.getOutputPrimaryModel().get().add(inferenceModel.difference(inputPrimaryModel));
	}
}
