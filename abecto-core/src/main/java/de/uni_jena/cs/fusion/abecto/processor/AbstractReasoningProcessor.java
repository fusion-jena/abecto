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

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;

/**
 * Provides a skeleton for reasoning {@link Processor Processors} that takes
 * care of the execution and inference output storing of a reasoner provided by
 * the method {@link #getReasoner()}.
 */
public abstract class AbstractReasoningProcessor<P extends Processor<P>> extends Processor<P> {

	@Override
	public final void run() {
		Resource dataset = this.getAssociatedDataset().orElseThrow();
		Reasoner reasoner = getReasoner();
		InfModel infModel = ModelFactory.createInfModel(reasoner, this.getInputPrimaryModelUnion(dataset));
		this.getOutputPrimaryModel().orElseThrow().add(infModel.getDeductionsModel());
	}

	/**
	 * Returns the reasoner to use.
	 * 
	 * @return the reasoner to use
	 */
	public abstract Reasoner getReasoner();

}
