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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.Parameter;

public class SparqlConstructProcessor extends Processor<SparqlConstructProcessor> {

	@Parameter
	public Query query;
	@Parameter
	public Integer maxIterations = 1;

	@Override
	public void run() {		
		Model outputPrimaryModel = this.getOutputPrimaryModel().get();
		Model primaryModelUnion = this.getPrimaryModelUnion();

		for (int iteration = 1; iteration <= maxIterations; iteration++) {
			// prepare execution
			QueryExecution queryExecution = QueryExecutionFactory.create(query, primaryModelUnion);

			// execute and write into intermediate result model
			Model intermediateResultModel = queryExecution.execConstruct(ModelFactory.createDefaultModel());

			// add new statements (if any) to result model, otherwise break
			if (!primaryModelUnion.containsAll(intermediateResultModel)) {
				outputPrimaryModel.add(intermediateResultModel);
			} else {
				break;
			}
		}
	}
}
