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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.processor.AbstractDeviationProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class ResourceDeviationProcessor extends AbstractDeviationProcessor<AbstractDeviationProcessor.Parameter> {

	@Override
	public Collection<Deviation> computeDeviations(Model model1, Model model2, UUID ontologyId1,
			UUID ontologyId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception {

		Collection<Deviation> deviations = new ArrayList<>();

		Map<Resource, Map<String, Resource>> valuesByVariableByResource2 = new HashMap<>();
		ResultSet results2 = category2.selectCategory(model2);
		while (results2.hasNext()) {
			QuerySolution result2 = results2.next();
			Resource resource2 = result2.getResource(categoryName);

			Map<String, Resource> valuesByVariable2 = valuesByVariableByResource2.computeIfAbsent(resource2, (x) -> {
				return new HashMap<>();
			});
			// iterate variables
			for (String variableName : variableNames) {
				if (result2.contains(variableName)) {
					try {
						valuesByVariable2.put(variableName, result2.getResource(variableName));
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(ontologyId2, resource2, variableName, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
					}
				}
			}
		}

		ResultSet results1 = category1.selectCategory(model1);
		while (results1.hasNext()) {
			QuerySolution result1 = results1.next();
			Resource resource1 = result1.getResource(categoryName);
			// iterate variables
			for (String variableName : variableNames) {
				if (result1.contains(variableName)) {
					try {
						Resource value1 = result1.getResource(variableName);
						for (Resource resource2 : mappings.getOrDefault(resource1, Collections.emptySet())) {
							if (valuesByVariableByResource2.containsKey(resource2)) {
								Resource value2 = valuesByVariableByResource2.get(resource2).get(variableName);
								if (value2 != null
										&& (!mappings.containsKey(value1) || !mappings.get(value1).contains(value2))) {
									deviations.add(new Deviation(null, categoryName, variableName, resource1, resource2,
											ontologyId1, ontologyId2, "<" + value1.getURI() + ">",
											"<" + value2.getURI() + ">"));
								}
							}
						}
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(ontologyId1, resource1, variableName, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
					}
				}
			}
		}

		return deviations;
	}

}
