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
import java.util.HashSet;
import java.util.List;
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.processor.DeviationProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class LiteralDeviationProcessor extends DeviationProcessor<DeviationProcessor.Parameter> {

	@JsonIgnore
	public static Map<Resource, Map<String, Set<Literal>>> getVariables(Category category, UUID ontologyId, Model model,
			Collection<String> variables, Collection<Issue> issueSink) {
		// TODO cache results
		ResultSet solutions = category.selectCategory(model);
		List<String> variablesToUse = solutions.getResultVars();
		variablesToUse.remove(category.name);
		variablesToUse.retainAll(variables);
		Map<Resource, Map<String, Set<Literal>>> results = new HashMap<>();
		while (solutions.hasNext()) {
			QuerySolution solution = solutions.next();
			Resource resource = solution.getResource(category.name);
			Map<String, Set<Literal>> result = results.computeIfAbsent(resource, (x) -> new HashMap<>());
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						result.computeIfAbsent(variable, (x) -> new HashSet<Literal>())
								.add(solution.getLiteral(variable));
					} catch (ClassCastException e) {
						if (issueSink != null) {
							issueSink.add(Issue.unexpectedValueType(ontologyId, resource, variable, "literal"));
						} else {
							throw e;
						}
					}
				}
			}
		}
		return results;
	}

	@Override
	public Collection<Deviation> computeDeviations(Model model1, Model model2, UUID ontologyId1, UUID ontologyId2,
			String categoryName, Collection<String> variableNames, Category category1, Category category2,
			Map<Resource, Set<Resource>> mappings) throws Exception {

		Collection<Deviation> deviations = new ArrayList<>();
		Collection<Issue> issues = new ArrayList<>();

		Map<Resource, Map<String, Set<Literal>>> valuesByVariableByResource1, valuesByVariableByResource2;
		valuesByVariableByResource1 = getVariables(category1, ontologyId1, model1, variableNames, issues);
		valuesByVariableByResource2 = getVariables(category2, ontologyId2, model2, variableNames, issues);

		for (Resource resource1 : valuesByVariableByResource1.keySet()) {
			for (Resource resource2 : mappings.getOrDefault(resource1, Collections.emptySet())) {
				for (String variableName : variableNames) {
					Set<Literal> literals1 = valuesByVariableByResource1.get(resource1).get(variableName);
					Set<Literal> literals2 = valuesByVariableByResource2.getOrDefault(resource2, Collections.emptyMap())
							.get(variableName);
					if (literals1 != null && literals2 != null) {
						deviations.addAll(compare(categoryName, variableName, resource1, resource2, ontologyId1,
								ontologyId2, literals1, literals2));
					}
				}
			}
		}

		SparqlEntityManager.insert(issues, this.getResultModel());

		return deviations;
	}

	private static Collection<Deviation> compare(String categoryName, String variableName, Resource resource1,
			Resource resource2, UUID ontologyId1, UUID ontologyId2, Collection<Literal> literals1,
			Collection<Literal> literals2) {
		Collection<Deviation> deviations = new ArrayList<>();
		Collection<Literal> unmatchedLiterals1 = new HashSet<>(literals1);
		Collection<Literal> unmatchedLiterals2 = new HashSet<>(literals2);
		// compare all literals
		for (Literal literal1 : literals1) {
			for (Literal literal2 : literals2) {
				if (equals(literal1, literal2)) {
					unmatchedLiterals1.remove(literal1);
					unmatchedLiterals2.remove(literal2);
				}
			}
		}
		// report not matched literals
		for (Literal literal1 : literals1) {
			for (Literal literal2 : unmatchedLiterals2) {
				deviations.add(new Deviation(null, categoryName, variableName, resource1, resource2, ontologyId1,
						ontologyId2, literal1.toString(), literal2.toString()));
			}
		}
		for (Literal literal2 : literals2) {
			for (Literal literal1 : unmatchedLiterals1) {
				deviations.add(new Deviation(null, categoryName, variableName, resource1, resource2, ontologyId1,
						ontologyId2, literal1.toString(), literal2.toString()));
			}
		}
		return deviations;
	}

	private static boolean equals(Literal literal1, Literal literal2) {
		// same type/subtype check
		if (literal1.sameValueAs(literal2)) {
			return true;
		}

		RDFDatatype type1 = literal1.getDatatype();
		RDFDatatype type2 = literal2.getDatatype();

		// comparison of different number types
		try {
			BigDecimal value1, value2;

			// get precise BigDecimal of literal 1 and handle special cases of float/double
			if (type1 instanceof XSDBaseNumericType) {
				value1 = new BigDecimal(literal1.getLexicalForm());
			} else if (type1 instanceof XSDDouble) {
				double value1Double = literal1.getDouble();
				// handle special cases
				if (value1Double == Double.NaN) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.NaN;
				} else if (value1Double == Double.NEGATIVE_INFINITY) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.NEGATIVE_INFINITY;
				} else if (value1Double == Double.POSITIVE_INFINITY) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.POSITIVE_INFINITY;
				}
				// get value as BigDecimal
				value1 = new BigDecimal(value1Double);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value1Double) or new
				 * BigDecimal(literal1.getLexicalForm()) to represented value from the double
				 * value space, not the double lexical space
				 */
			} else if (type1 instanceof XSDFloat) {
				float value1Float = literal1.getFloat();
				// handle special cases
				if (value1Float == Double.NaN) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.NaN;
				} else if (value1Float == Double.NEGATIVE_INFINITY) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.NEGATIVE_INFINITY;
				} else if (value1Float == Double.POSITIVE_INFINITY) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.POSITIVE_INFINITY;
				}
				// get value as BigDecimal
				value1 = new BigDecimal(value1Float);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value1Float) or new
				 * BigDecimal(literal1.getLexicalForm()) to represented value from the float
				 * value space, not the float lexical space
				 */
			} else {
				return false;
			}

			// get precise BigDecimal of literal 2
			if (type2 instanceof XSDBaseNumericType) {
				value2 = new BigDecimal(literal2.getLexicalForm());
			} else if (type2 instanceof XSDDouble) {
				double value2Double = literal2.getDouble();
				// handle special cases
				if (value2Double == Double.NaN || value2Double == Double.NEGATIVE_INFINITY
						|| value2Double == Double.POSITIVE_INFINITY) {
					return false;
				}
				// get value as BigDecimal
				value2 = new BigDecimal(value2Double);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value2Double) or new
				 * BigDecimal(literal2.getLexicalForm()) to represented value from the double
				 * value space, not the double lexical space
				 */
			} else if (type2 instanceof XSDFloat) {
				float value2Float = literal2.getFloat();
				// handle special cases
				if (value2Float == Float.NaN || value2Float == Float.NEGATIVE_INFINITY
						|| value2Float == Float.POSITIVE_INFINITY) {
					return false;
				}
				// get value as BigDecimal
				value2 = new BigDecimal(value2Float);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value2Float) or new
				 * BigDecimal(literal2.getLexicalForm()) to represented value from the float
				 * value space, not the float lexical space
				 */
			} else {
				return false;
			}

			// compare BigDecimals
			return value1.compareTo(value2) == 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
