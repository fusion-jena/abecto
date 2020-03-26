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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractTransformationProcessor;

public class SparqlConstructProcessor extends AbstractTransformationProcessor<SparqlConstructProcessor.Parameter> {

	@Override
	public void computeResultModel() {
		// prepare query
		Query query = QueryFactory.create(this.getParameters().query);

		// prepare execution
		QueryExecution queryExecution = QueryExecutionFactory.create(query, this.inputModelUnion);

		// execute write into result model
		queryExecution.execConstruct(this.getResultModel());
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public String query;
	}
}
