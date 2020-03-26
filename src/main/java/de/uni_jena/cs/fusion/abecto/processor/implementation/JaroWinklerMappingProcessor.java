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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.similarity.jarowinkler.JaroWinklerSimilarity;

public class JaroWinklerMappingProcessor extends AbstractMappingProcessor<JaroWinklerMappingProcessor.Parameter> {

	// TODO add language handling parameter

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public double threshold;
		public boolean case_sensitive;
		public String category;
		public Collection<String> variables;
	}

	/** the patterns relevant for the category mapped with contained variables */
	private Map<Category, Collection<Var>> patterns = new HashMap<>();
	/** the variable values mapped with the entities of the category by model */
	private Map<Model, Map<Var, Map<String, Collection<Resource>>>> values = new HashMap<>();

	private Map<Var, Map<String, Collection<Resource>>> getValues(Model model, boolean caseSensitive) {
		if (!this.values.containsKey(model)) {
			// do it only once for this processor instance

			// get values map for the model
			Map<Var, Map<String, Collection<Resource>>> modelValues = this.values.computeIfAbsent(model,
					m -> new HashMap<>());

			// get the processed category
			String category = this.getParameters().category;

			// iterate the patterns
			for (Entry<Category, Collection<Var>> entry : patterns.entrySet()) {
				Category pattern = entry.getKey();

				// get pattern variables except of category
				Collection<Var> variables = entry.getValue();
				variables.removeIf(variable -> variable.toString().equals(category));

				// add value maps for each variable
				variables.forEach((variable -> modelValues.computeIfAbsent(variable, v -> new HashMap<>())));

				// execute query of the pattern
				ResultSet results = pattern.selectCategory(model);

				// iterate results
				while (results.hasNext()) {
					QuerySolution result = results.next();

					// get resource of the result
					Resource entity = result.get(category).asResource();

					// iterate result variables
					for (Var variable : variables) {
						if (result.contains(variable.getVarName())) {
							String value = result.get(variable.getVarName()).toString();

							// add result variable value to values
							modelValues.get(variable).computeIfAbsent(value, v -> new HashSet<>()).add(entity);
						}
					}
				}

			}
		}
		return values.get(model);
	}

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID knowledgeBaseId1, UUID knowledgeBaseId2)
			throws ParseException, IllegalStateException, NullPointerException, ReflectiveOperationException {
		// get parameters
		boolean caseSensitive = this.getParameters().case_sensitive;
		double threshold = this.getParameters().threshold;
		Collection<String> variables = this.getParameters().variables;

		// get patterns
		for (Category category : SparqlEntityManager.select(new Category(), this.metaModel)) {
			Collection<Var> relevantVariables = category.getPatternVariables().stream()
					.filter((var) -> variables.contains(var.getName())).collect(Collectors.toList());
			if (!relevantVariables.isEmpty()) {
				this.patterns.put(category, relevantVariables);
			}
		}

		// get values
		Map<Var, Map<String, Collection<Resource>>> valuesByVariable1 = getValues(model1, caseSensitive);
		Map<Var, Map<String, Collection<Resource>>> valuesByVariable2 = getValues(model2, caseSensitive);

		// prepare mappings collection
		Collection<Mapping> mappings = new ArrayList<>();

		// iterate variables
		for (Var variable : valuesByVariable1.keySet()) {
			if (valuesByVariable1.containsKey(variable) && valuesByVariable2.containsKey(variable)) {

				Map<String, Collection<Resource>> values1 = valuesByVariable1.get(variable);
				Map<String, Collection<Resource>> values2 = valuesByVariable2.get(variable);

				JaroWinklerSimilarity<String> matcher1 = JaroWinklerSimilarity.with(values1.keySet(), threshold);
				JaroWinklerSimilarity<String> matcher2 = JaroWinklerSimilarity.with(values2.keySet(), threshold);

				// match from first to second
				Map<String, Collection<String>> matches1 = new HashMap<>();
				for (String value1 : values1.keySet()) {
					matches1.put(value1, maxValue(matcher2.apply(value1)));
				}

				// match from second to first
				for (String value2 : values2.keySet()) {
					for (String value1 : maxValue(matcher1.apply(value2))) {
						if (matches1.get(value1).contains(value2)) { // is bidirectional match
							/*
							 * NOTE: bidirectional matches are required to make the processor commutative
							 * regarding knowledge base order
							 */

							// convert match into mappings
							for (Resource resource1 : values1.get(value1)) {
								for (Resource resource2 : values2.get(value2)) {
									mappings.add(Mapping.of(resource1, resource2));
								}
							}
						}
					}
				}
			}
		}

		return mappings;
	}

	private Collection<String> maxValue(Map<String, Double> map) {
		List<String> bestMatches = new ArrayList<>();
		double maxSimilarity = 0d;
		for (Entry<String, Double> entry : map.entrySet()) {
			if (entry.getValue() < maxSimilarity) {
				// do nothing
			} else if (entry.getValue().equals(maxSimilarity)) {
				bestMatches.add(entry.getKey());
			} else {
				bestMatches.clear();
				maxSimilarity = entry.getValue();
				bestMatches.add(entry.getKey());
			}
		}
		return bestMatches;
	}
}