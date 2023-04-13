/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.jsonldjava.shaded.com.google.common.base.Objects;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;

public class PropertyComparisonProcessor extends Processor<PropertyComparisonProcessor> {

	private final Collection<String> LANGUAGE_FILTER_PATTERN_DEFAULT = List.of("","*");

	/** Aspect to process. */
	@Parameter
	public Resource aspect;
	/** Variables to process. */
	@Parameter
	public List<String> variables;
	/**
	 * Language patterns to filter compared literals. Literals of datatype xsd:string and
	 * rdf:langString will be considered only, if they match at least on of these patterns.
	 * String literals without language tag will match with "", all string literals with
	 * language tag match with "*". Default: "","*" (all match)
	 */
	@Parameter
	public Collection<String> languageFilterPatterns = LANGUAGE_FILTER_PATTERN_DEFAULT;
	/**
	 * If true, a literal of the type xsd:date and a literal of the type
	 * xsd:dateTime with equal year, month and day part will match.
	 */
	@Parameter
	public boolean allowTimeSkip;
	/**
	 * If true, literals of the type xsd:string or rdf:langString with equal lexical
	 * value but different language tag will match.
	 */
	@Parameter
	public boolean allowLangTagSkip;



	/**
	 * Digits to preserve when rounding after division in measurement calculations.
	 */
	public final static int SCALE = 16;

	/**
	 * Returns a duplicate free list of the given values by removing all values equivalent to an earlier value.
	 */
	private List<RDFNode> deduplicate(Iterable<RDFNode> values) {
		ArrayList<RDFNode> distinctValues = new ArrayList<>();
		for (RDFNode value : values) {
			if (distinctValues.stream().noneMatch(v -> equivalentValues(v, value))) {
				distinctValues.add(value);
			}
		}
		return distinctValues;
	}

	private <T> Set<T> distinctByIdentity(Collection<T> items) {
		Set<T> distinctItems = Collections.newSetFromMap(new IdentityHashMap<>());
		distinctItems.addAll(items);
		return distinctItems;
	}

	private int getPairwiseOverlap(Collection<Resource> resources1,
									Collection<Resource> resources2, Map<RDFNode, Set<Resource>> resourcesByMappedValues) {
		int pairwiseOverlap = 0;
		// use set of resource sets for mapping values
		// NOTE: equivalent values use the same set, so get distinct set instances
		for (Set<Resource> resourceSet : distinctByIdentity(resourcesByMappedValues.values())) {
			if (resources1.stream().noneMatch(resourceSet::contains)) {
				continue;
			}
			if (resources2.stream().noneMatch(resourceSet::contains)) {
				continue;
			}
			pairwiseOverlap++;
		}
		return pairwiseOverlap;
	}

	/** Removes all values that are known wrong values. */
	private void removeKnownWrongValues(Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource, Resource dataset) {
		for (Resource resource : valuesByVariableByResource.keySet()) {
			removeKnownWrongValues(valuesByVariableByResource.get(resource), resource, dataset);
		}
	}

	/** Removes all values that are known wrong values. */
	private void removeKnownWrongValues(Map<String, Set<RDFNode>> valuesByVariable, Resource resource, Resource dataset) {
		Model inputMetaModel = this.getInputMetaModelUnion(dataset);
		for (String variable : valuesByVariable.keySet()) {
			Set<RDFNode> values = valuesByVariable.get(variable);
			values.removeIf(value -> Metadata.isWrongValue(resource, variable, value, aspect, inputMetaModel));
		}
	}

	@Override
	public final void run() {
		// Number of covered values of another dataset, per variable. Index: variable, affectedDataset, comparedToDataset
		Map<String, Map<Resource, Map<Resource, Integer>>> absoluteCoverage = new HashMap<>();
		// Number of values in this dataset, per variable. Index: variable, affectedDataset
		Map<String, Map<Resource, Integer>> count = new HashMap<>();
		// Number of distinct values in this dataset, per variable. Index: variable, affectedDataset
		Map<String, Map<Resource, Integer>> deduplicatedCount = new HashMap<>();
		// Number of overlaps between all pairs of dataset by dataset compared to by multiplied by 2.
		Map<String, Integer> totalPairwiseOverlapByVariable = new HashMap<>();
		// Estimated population site of values, per variable.
		Map<String, BigDecimal> populationSize = new HashMap<>();

		Aspect aspect = this.getAspects().get(this.aspect);

		// init count and countDistinct
		for (String variable : variables) {
			count.put(variable, new HashMap<>());
			deduplicatedCount.put(variable, new HashMap<>());
		}

		for (Resource dataset : aspect.getDatasets()) {
			Model model = this.getInputPrimaryModelUnion(dataset);

			Aspect.getResourceKeys(aspect, dataset, model).forEach(resource -> {
				// get resource values
				Map<String, Set<RDFNode>> valuesByVariable = aspect.selectResourceValues(resource, dataset, variables,
						model);

				// removeExcludedValues
				valuesByVariable.forEach((k,v) -> v.removeIf(this::isExcludedValue));

				for (String variable : variables) {
					// measure count of values
					count.get(variable).merge(dataset,
							valuesByVariable.getOrDefault(variable, Collections.emptySet()).size(), Integer::sum);

					// measure count of non equivalent values per resource
					deduplicatedCount.get(variable).merge(dataset,
							deduplicate(valuesByVariable.getOrDefault(variable, Collections.emptySet())).size(),
							Integer::sum);
				}

			});
		}

		getCorrespondenceGroups(aspect.getIri()).forEach(correspondingResources -> {
			// get values for all corresponding resources in all datasets
			Map<Resource,Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset = new HashMap<>();
			for (Resource dataset : aspect.getDatasets()) {
				Model model = this.getInputPrimaryModelUnion(dataset);
				valuesByVariableByResourceByDataset.put(dataset, aspect
						.selectResourceValues(correspondingResources, dataset, variables, model));
				removeKnownWrongValues(valuesByVariableByResourceByDataset.get(dataset), dataset);
				// removeExcludedValues
				valuesByVariableByResourceByDataset.forEach((k3,v3) -> v3.forEach((k2,v2) -> v2.forEach((k,v) -> v.removeIf(this::isExcludedValue))));
				// update deduplicated counts
				for (String variable : variables) {
					// get values of all corresponding resources
					var valuesOfCorrespondingResources = new ArrayList<RDFNode>();
					valuesByVariableByResourceByDataset.get(dataset).values().stream()
							.map(m -> m.getOrDefault(variable, Collections.emptySet()))
							.map(this::deduplicate) // avoid correction of count twice?
							.forEach(valuesOfCorrespondingResources::addAll);

					// measure count of non equivalent values per corresponding resources
					deduplicatedCount.get(variable).merge(dataset,
							deduplicate(valuesOfCorrespondingResources).size() - valuesOfCorrespondingResources.size(),
							Integer::sum);
				}
			}

			for (Resource dataset1 : aspect.getDatasets()) {
				Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1 = valuesByVariableByResourceByDataset.get(dataset1);

				for (Resource dataset2 : aspect.getDatasets()) {
					// do not do work twice
					if (dataset1.hashCode() < dataset2.hashCode()) continue;

					Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource2 = valuesByVariableByResourceByDataset.get(dataset2);
					for (String variable : variables) {
						if (aspect.getPattern(dataset1).getResultVars().contains(variable)
								&& aspect.getPattern(dataset2).getResultVars().contains(variable)) {
							this.compareVariableValues(variable, dataset1, valuesByVariableByResource1, dataset2,
									valuesByVariableByResource2, absoluteCoverage);
						}
					}
				}

			}
		});

		// calculate total pairwise overlaps
		for (String variable : variables) {
			int totalPairwiseOverlap = 0;
			for (Resource dataset : aspect.getDatasets()) {
				for (Resource datasetComparedTo : aspect.getDatasets()) {
					if (dataset.hashCode() < datasetComparedTo.hashCode()) { // only once per pair
						totalPairwiseOverlap += absoluteCoverage
								.getOrDefault(variable,Collections.emptyMap())
								.getOrDefault(dataset,Collections.emptyMap())
								.getOrDefault(datasetComparedTo,0);
					}
				}
			}
			totalPairwiseOverlapByVariable.put(variable, totalPairwiseOverlap);
		}

		// calculate estimated value population size and estimated completeness per variable
		for (String variable : variables) {
			populationSize.put(variable, BigDecimal.ZERO);
			if (totalPairwiseOverlapByVariable.containsKey(variable) // variable covered by more than 1 dataset
					&& totalPairwiseOverlapByVariable.get(variable) != 0) {

				// calculate estimated value population size per variable
				for (Resource affectedDataset : aspect.getDatasets()) {
					for (Resource comparedToDataset : aspect.getDatasets()) {

						// only once per pair
						// do not use Resource#getURI() as it might be null for blank nodes
						if (affectedDataset.hashCode() >= comparedToDataset.hashCode()) continue;

						populationSize.merge(variable,
								BigDecimal.valueOf(deduplicatedCount.get(variable).get(affectedDataset)).multiply(
										BigDecimal.valueOf(deduplicatedCount.get(variable).get(comparedToDataset))),
								BigDecimal::add);
					}
				}
				populationSize.merge(variable, BigDecimal.valueOf(totalPairwiseOverlapByVariable.get(variable)),
						(x, y) -> x.divide(y, SCALE, RoundingMode.HALF_UP));

				// calculate & store estimated completeness
				for (Resource affectedDataset : aspect.getDatasets()) {
					// calculate ratio of values in an estimated population covered by this dataset
					BigDecimal completeness = BigDecimal.valueOf(deduplicatedCount.get(variable).get(affectedDataset))
							.divide(populationSize.get(variable), SCALE, RoundingMode.HALF_UP);
					Collection<Resource> otherDatasets = new HashSet<>(aspect.getDatasets());
					otherDatasets.remove(affectedDataset);
					// TODO add value exclusion filter description to measurement description
					Metadata.addQualityMeasurement(AV.marCompletenessThomas08, completeness, OM.one, affectedDataset,
							variable, otherDatasets, aspect.getIri(), this.getOutputMetaModel(affectedDataset));
				}
			}
		}

		// store measurements
		for (String variable : variables) {

			for (Resource affectedDataset : aspect.getDatasets()) {
				// skip if variable not covered by affected dataset
				if (!aspect.getPattern(affectedDataset).getResultVars().contains(variable)) continue;

				// store count
				// TODO add value exclusion filter description to measurement description
				Metadata.addQualityMeasurement(AV.count, count.get(variable).getOrDefault(affectedDataset, 0), OM.one,
						affectedDataset, variable, aspect.getIri(), this.getOutputMetaModel(affectedDataset));
				// store deduplicated count
				// TODO add value exclusion filter description to measurement description
				Metadata.addQualityMeasurement(AV.deduplicatedCount,
						deduplicatedCount.get(variable).getOrDefault(affectedDataset, 0), OM.one, affectedDataset, variable,
						aspect.getIri(), this.getOutputMetaModel(affectedDataset));

				// calculate relative coverage
				for (Resource comparedToDataset : aspect.getDatasets()) {
					if (affectedDataset.equals(comparedToDataset)) continue;

					// skip if variable not covered by dataset compared to
					if (!aspect.getPattern(comparedToDataset).getResultVars().contains(variable)) continue;

					// store absolute coverage
					int overlap = absoluteCoverage.getOrDefault(variable, Collections.emptyMap())
							.getOrDefault(affectedDataset, Collections.emptyMap())
							.getOrDefault(comparedToDataset, 0);
					// TODO add value exclusion filter description to measurement description
					Metadata.addQualityMeasurement(AV.absoluteCoverage, overlap,
							OM.one, affectedDataset, variable, comparedToDataset, aspect.getIri(),
							this.getOutputMetaModel(affectedDataset));

					// calculate & store relative coverage
					int countComparedTo = deduplicatedCount.getOrDefault(variable, Collections.emptyMap())
							.getOrDefault(comparedToDataset, 0);
					if (countComparedTo != 0) {
						// TODO add value exclusion filter description to measurement description
						Metadata.addQualityMeasurement(AV.relativeCoverage, BigDecimal.valueOf(overlap)
										.divide(BigDecimal.valueOf(countComparedTo), SCALE, RoundingMode.HALF_UP),
								OM.one, affectedDataset, variable, comparedToDataset, aspect.getIri(),
								this.getOutputMetaModel(affectedDataset));
					}
				}
			}
		}
	}

	/**
	 * 
	 * Note: Not the most efficient way to do this, but there is no
	 * {@link Comparator} available to use {@link TreeMap#TreeMap(Comparator)}.
	 *
	 */
	private void mapResources(String variable, Map<RDFNode, Set<Resource>> resourcesByMappedValues,
			Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource) {
		for (Resource resource : valuesByVariableByResource.keySet()) {
			for (RDFNode value : valuesByVariableByResource.get(resource).getOrDefault(variable,
					Collections.emptySet())) {
				for (RDFNode valueKey : resourcesByMappedValues.keySet()) {
					if (equivalentValues(value, valueKey)) {
						Set<Resource> valuesSet = resourcesByMappedValues.get(valueKey);
						// add values to existing set
						valuesSet.add(resource);
						// map value to same set, so equivalent values will share one set
						resourcesByMappedValues.putIfAbsent(value, valuesSet);
						break;
					}
				}
				// no equivalent value in map
				resourcesByMappedValues.computeIfAbsent(value, v -> new HashSet<>()).add(resource);
			}
		}
	}

	/**
	 * Compares the values of one variable from two corresponding resources in two
	 * datasets and stores encountered deviations and issues in both according
	 * outputMetaModels (derived by {@link #getOutputMetaModel(Resource)}). Either
	 * the corresponding resources or the datasets might be equal, but not both at
	 * once.
	 *
	 * @param variable                    Name of the compared variable
	 * @param dataset1                    IRI of the first dataset
	 * @param valuesByVariableByResource1 Values of the first datasets resources
	 * @param dataset2                    IRI of the second datasets dataset
	 * @param valuesByVariableByResource2 Values of the second datasets resources
	 */
	public void compareVariableValues(String variable, Resource dataset1,
									  Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1, Resource dataset2,
									  Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource2, Map<String, Map<Resource, Map<Resource, Integer>>> absoluteCoverage) {

		// create common value-resource look-up
		Map<RDFNode, Set<Resource>> resourcesByMappedValues = new HashMap<>();
		mapResources(variable, resourcesByMappedValues, valuesByVariableByResource1);
		mapResources(variable, resourcesByMappedValues, valuesByVariableByResource2);

		// update measurements
		if (!dataset1.equals(dataset2)) {
			int pairwiseOverlap = getPairwiseOverlap(valuesByVariableByResource1.keySet(), valuesByVariableByResource2.keySet(), resourcesByMappedValues);
			absoluteCoverage.computeIfAbsent(variable, v -> new HashMap<>()).computeIfAbsent(dataset1, v -> new HashMap<>())
					.merge(dataset2, pairwiseOverlap, Integer::sum);
			absoluteCoverage.computeIfAbsent(variable, v -> new HashMap<>()).computeIfAbsent(dataset2, v -> new HashMap<>())
					.merge(dataset1, pairwiseOverlap, Integer::sum);
		}

		// deviation: a pair of resources with each having a value not present in the
		// other resource
		// omission: a pair of resources with one having a value not present in the other,
		// but not vice versa

		for (Resource resource1 : valuesByVariableByResource1.keySet()) {
			var values1 = valuesByVariableByResource1.get(resource1).getOrDefault(variable, Collections.emptySet());
			for (Resource resource2 : valuesByVariableByResource2.keySet()) {
				var values2 = valuesByVariableByResource2.get(resource2).getOrDefault(variable, Collections.emptySet());

				var notMatchingValues1 = values1.stream().filter(value1 -> !resourcesByMappedValues
						.getOrDefault(value1, Collections.emptySet()).contains(resource2)).collect(Collectors.toList());
				var notMatchingValues2 = values2.stream().filter(value2 -> !resourcesByMappedValues
						.getOrDefault(value2, Collections.emptySet()).contains(resource1)).collect(Collectors.toList());

				// report missing not matching values
				if (notMatchingValues1.isEmpty()) {
					for (RDFNode value2 : notMatchingValues2) {
						Metadata.addValuesOmission(resource1, variable, dataset2, resource2, value2, this.aspect,
								this.getOutputMetaModel(dataset1));
					}
				} else if (notMatchingValues2.isEmpty()) {
					for (RDFNode value1 : notMatchingValues1) {
						Metadata.addValuesOmission(resource2, variable, dataset1, resource1, value1, this.aspect,
								this.getOutputMetaModel(dataset2));
					}
				} else {
					// report pairs of deviating values
					for (RDFNode value1 : notMatchingValues1) {
						for (RDFNode value2 : notMatchingValues2) {
							Metadata.addDeviation(resource1.asResource(), variable, value1, dataset2,
									resource2.asResource(), value2, this.aspect, this.getOutputMetaModel(dataset1));
							Metadata.addDeviation(resource2.asResource(), variable, value2, dataset1,
									resource1.asResource(), value1, this.aspect, this.getOutputMetaModel(dataset2));
						}
					}
				}
			}
		}
	}

	/**
	 * Checks if two values are equivalent.
	 * 
	 * @param value1 the first value to compare
	 * @param value2 the second value to compare
	 * @return {@code true}, if the values are equivalent, otherwise {@code false}
	 */
	public boolean equivalentValues(RDFNode value1, RDFNode value2) {
		if (value1.isResource() && value2.isResource()) {
			return correspond(value1.asResource(), value2.asResource());
		} else if (value1.isLiteral() && value2.isLiteral()) {
			Literal literal1 = value1.asLiteral();
			Literal literal2 = value2.asLiteral();

			// same type/subtype check
			if (literal1.sameValueAs(literal2)) {
				return true;
			}

			RDFDatatype type1 = literal1.getDatatype();
			RDFDatatype type2 = literal2.getDatatype();

			// comparison of xsd:date and xsd:dateTime
			if (allowTimeSkip && (type1 instanceof XSDDateType && type2 instanceof XSDDateTimeType
					|| type1 instanceof XSDDateTimeType && type2 instanceof XSDDateType)) {
				XSDDateTime date1 = ((XSDDateTime) literal1.getValue());
				XSDDateTime date2 = ((XSDDateTime) literal2.getValue());
				return date1.getDays() == date2.getDays() //
						&& date1.getMonths() == date2.getMonths() //
						&& date1.getYears() == date2.getYears();
			}

			// ignore lang tags
			if (allowLangTagSkip && (type1 instanceof XSDBaseStringType || type1 instanceof RDFLangString)
					&& (type2 instanceof XSDBaseStringType || type2 instanceof RDFLangString)) {
				String string1 = literal1.getString();
				String string2 = literal2.getString();
				return Objects.equal(string1, string2);
			}

			// comparison of different number types
			try {
				BigDecimal decimal1, decimal2;

				// get precise BigDecimal of literal 1 and handle special cases of float/double
				if (type1 instanceof XSDBaseNumericType) {
					decimal1 = new BigDecimal(literal1.getLexicalForm());
				} else if (type1 instanceof XSDDouble) {
					double value1Double = literal1.getDouble();
					// handle special cases
					if (Double.isNaN(value1Double)) {
						return type2 instanceof XSDFloat && Float.isNaN(literal2.getFloat());
					} else if (value1Double == Double.NEGATIVE_INFINITY) {
						return type2 instanceof XSDFloat && literal2.getFloat() == Float.NEGATIVE_INFINITY;
					} else if (value1Double == Double.POSITIVE_INFINITY) {
						return type2 instanceof XSDFloat && literal2.getFloat() == Float.POSITIVE_INFINITY;
					}
					// get value as BigDecimal
					decimal1 = new BigDecimal(value1Double);
					/*
					 * NOTE: don't use BigDecimal#valueOf(value1Double) or new
					 * BigDecimal(literal1.getLexicalForm()) to represented value from the double
					 * value space, not the double lexical space
					 */
				} else if (type1 instanceof XSDFloat) {
					float value1Float = literal1.getFloat();
					// handle special cases
					if (Float.isNaN(value1Float)) {
						return type2 instanceof XSDDouble && Double.isNaN(literal2.getDouble());
					} else if (value1Float == Double.NEGATIVE_INFINITY) {
						return type2 instanceof XSDDouble && literal2.getDouble() == Double.NEGATIVE_INFINITY;
					} else if (value1Float == Double.POSITIVE_INFINITY) {
						return type2 instanceof XSDDouble && literal2.getDouble() == Double.POSITIVE_INFINITY;
					}
					// get value as BigDecimal
					decimal1 = new BigDecimal(value1Float);
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
					decimal2 = new BigDecimal(literal2.getLexicalForm());
				} else if (type2 instanceof XSDDouble) {
					double value2Double = literal2.getDouble();
					// handle special cases
					if (Double.isNaN(value2Double) || value2Double == Double.NEGATIVE_INFINITY
							|| value2Double == Double.POSITIVE_INFINITY) {
						return false;
					}
					// get value as BigDecimal
					decimal2 = new BigDecimal(value2Double);
					/*
					 * NOTE: don't use BigDecimal#valueOf(value2Double) or new
					 * BigDecimal(literal2.getLexicalForm()) to represented value from the double
					 * value space, not the double lexical space
					 */
				} else if (type2 instanceof XSDFloat) {
					float value2Float = literal2.getFloat();
					// handle special cases
					if (Float.isNaN(value2Float) || value2Float == Float.NEGATIVE_INFINITY
							|| value2Float == Float.POSITIVE_INFINITY) {
						return false;
					}
					// get value as BigDecimal
					decimal2 = new BigDecimal(value2Float);
					/*
					 * NOTE: don't use BigDecimal#valueOf(value2Float) or new
					 * BigDecimal(literal2.getLexicalForm()) to represented value from the float
					 * value space, not the float lexical space
					 */
				} else {
					return false;
				}

				// compare BigDecimals
				return decimal1.compareTo(decimal2) == 0;
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks if a value should be excluded from the comparison.
	 * 
	 * @param value the value to check
	 * @return {@code true}, if the value should not be used, otherwise {@code false}
	 */
	public boolean isExcludedValue(RDFNode value) {
		if (languageFilterPatterns == LANGUAGE_FILTER_PATTERN_DEFAULT ||
				!value.isLiteral() ||
				!(value.asLiteral().getDatatype() instanceof XSDBaseStringType) &&
						!(value.asLiteral().getDatatype() instanceof RDFLangString)) {
			return false;
		} else {
			String langStr = value.asLiteral().getLanguage();
			return languageFilterPatterns.stream()
					.noneMatch(languageFilterPattern -> NodeFunctions.langMatches(langStr, languageFilterPattern));
		}
	}
}
