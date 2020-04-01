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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class ManualCategoryProcessor extends AbstractMetaProcessor<ManualCategoryProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		/**
		 * The patterns by category name. The variable with the same name as the
		 * category will be assumed to be the primary key.
		 */
		public Map<String, String> patterns = new HashMap<>();
	}

	@Override
	protected void computeResultModel() throws Exception {
		Collection<Category> categories = new ArrayList<>();

		if (this.getParameters().patterns.isEmpty()) {
			throw new IllegalArgumentException("Empty pattern list.");
		}

		UUID ontology = this.getOntology();

		for (Entry<String, String> patternOfCategory : this.getParameters().patterns.entrySet()) {
			String categoryName = patternOfCategory.getKey();
			String categoryPattern = patternOfCategory.getValue();
			categories.add(new Category(categoryName, categoryPattern, ontology));
		}

		SparqlEntityManager.insert(categories, this.getResultModel());
	}

}
