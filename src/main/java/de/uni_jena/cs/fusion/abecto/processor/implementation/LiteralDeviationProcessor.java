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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.datatypes.xsd.impl.XSDDouble;
import org.apache.jena.datatypes.xsd.impl.XSDFloat;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.processor.AbstractDeviationProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class LiteralDeviationProcessor extends AbstractDeviationProcessor<AbstractDeviationProcessor.Parameter> {

	@Override
	public Collection<Deviation> computeDeviations(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception {

		Collection<Deviation> deviations = new ArrayList<>();

		Map<Resource, Map<String, Literal>> valuesByVariableByResource2 = new HashMap<>();
		ResultSet results2 = category2.selectCategory(model2);
		while (results2.hasNext()) {
			QuerySolution result2 = results2.next();
			Resource resource2 = result2.getResource(categoryName);

			Map<String, Literal> valuesByVariable2 = valuesByVariableByResource2.computeIfAbsent(resource2, (x) -> {
				return new HashMap<>();
			});
			// iterate variables
			for (String variableName : variableNames) {
				if (result2.contains(variableName)) {
					try {
						valuesByVariable2.put(variableName, result2.getLiteral(variableName));
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(knowledgeBaseId2, resource2, variableName, "literal");
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
						Literal value1 = result1.getLiteral(variableName);
						RDFDatatype type1 = value1.getDatatype();
						for (Resource resource2 : mappings.getOrDefault(resource1, Collections.emptySet())) {
							if (valuesByVariableByResource2.containsKey(resource2)) {
								Literal value2 = valuesByVariableByResource2.get(resource2).get(variableName);
								if (value2 != null) {
									// same type/subtype check
									if (value1.sameValueAs(value2))
										continue;
									// different number types check
									if (type1 instanceof XSDBaseNumericType || type1 instanceof XSDDouble
											|| type1 instanceof XSDFloat) {
										RDFDatatype type2 = value2.getDatatype();
										if ((type2 instanceof XSDBaseNumericType || type2 instanceof XSDDouble
												|| type2 instanceof XSDFloat)) {
											// compare as BigDecimal (comparing Floats as Double results in precision
											// problems for e.g. 0.001)
											BigDecimal bigDecimalValue1 = new BigDecimal(value1.getLexicalForm());
											BigDecimal bigDecimalValue2 = new BigDecimal(value2.getLexicalForm());
											if (bigDecimalValue1.compareTo(bigDecimalValue2) == 0) {
												continue;
											}
										}
									}
									deviations.add(new Deviation(null, categoryName, variableName, resource1, resource2,
											knowledgeBaseId1, knowledgeBaseId2, value1.toString(), value2.toString()));
								}
							}
						}
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(knowledgeBaseId1, resource1, variableName, "literal");
						SparqlEntityManager.insert(issue, this.getResultModel());
					}
				}
			}
		}

		return deviations;
	}

}
