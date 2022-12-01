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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

public abstract class AbstractValueComparisonProcessor<P extends Processor<P>> extends Processor<P> {

	/** Aspect to process. */
	@Parameter
	public Resource aspect;
	/** Variables to process. */
	@Parameter
	public List<String> variables;

	/**
	 * Number of covered values of another dataset, per variable.
	 * 
	 * Index: variable, affectedDataset, comparedToDataset
	 */
	private Map<String, Map<Resource, Map<Resource, Integer>>> absoluteCoverage = new HashMap<>();
	/**
	 * Ratio of covered values of another dataset, per variable.
	 * 
	 * Index: variable, affectedDataset, comparedToDataset
	 */
	private Map<String, Map<Resource, Map<Resource, BigDecimal>>> relativeCoverage = new HashMap<>();
	/**
	 * Number of values in this dataset, per variable.
	 * 
	 * Index: variable, affectedDataset
	 */
	private Map<String, Map<Resource, Integer>> count;
	/**
	 * Number of distinct values in this dataset, per variable.
	 * 
	 * Index: variable, affectedDataset
	 */
	private Map<String, Map<Resource, Integer>> countDistinct;
	/**
	 * Number of overlaps between all pairs of dataset by dataset compared to by
	 * multiplied by 2.
	 */
	private Map<String, Integer> totalPairwiseOverlapByVariable = new HashMap<>();
	/**
	 * Estimated population site of values, per variable.
	 */
	private Map<String, BigDecimal> populationSize = new HashMap<>();

	public final static int SCALE = 16;

	/**
	 * Returns a merge per dataset of all values of the given resources. Values that
	 * are invalid due to {@link #isValidValue(RDFNode)}, should not be used due to
	 * {@link #useValue(RDFNode)} ignored due to Invalid or are known to be wrong
	 * will not be added.
	 * 
	 * @return merge of the vaules per dataset; index: dataset, variable, value,
	 *         resources
	 */
	private Map<Resource, Map<String, Map<RDFNode, Set<Resource>>>> getValuesPerDataset(Collection<Resource> resources,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {
		// init result map
		Map<Resource, Map<String, Map<RDFNode, Set<Resource>>>> datasetValueMerges = new HashMap<>();
		// iterate datasets
		for (Resource dataset : valuesByVariableByResourceByDataset.keySet()) {
			var valuesByVariableByResource = valuesByVariableByResourceByDataset.get(dataset);
			// init result per dataset
			var valueMerges = datasetValueMerges.computeIfAbsent(dataset, d -> new HashMap<>());
			// iterate resources
			for (Resource resource : resources) {
				var valuesMap = valuesByVariableByResource.get(resource);
				if (valuesMap == null) {
					// resource is not in dataset
					continue;
				}
				// iterate variables
				for (String variable : valuesMap.keySet()) {
					Set<RDFNode> values = valuesMap.get(variable);
					if (values == null) {
						// variable not set for resource in dataset
						continue;
					}
					// iterate values
					for (RDFNode value : values) {

						if (!this.isValidValue(value)) {
							// report invalid value
							Metadata.addIssue(resource, variable, value, aspect, "Invalid Value",
									this.invalidValueComment(), this.getOutputMetaModel(dataset));
							// ignore value
							continue;
						}

						if (!this.useValue(value)) {
							// ignore value
							continue;
						}

						if (Metadata.isWrongValue(resource, variable, value, aspect,
								this.getInputMetaModelUnion(dataset))) {
							// ignore value
							continue;
						}

						valueMerges.computeIfAbsent(variable, v -> new HashMap<>())// add variable and value map
								.computeIfAbsent(value, v -> new HashSet<>())// add value and origin set
								.add(resource); // add origin resource
					}
				}
			}
		}
		return datasetValueMerges;
	}

	private void calculateCount(Set<Resource> datasets, Iterable<String> variables,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {

		// init count
		count = new HashMap<>();

		// calculate count
		for (String variable : variables) {
			var countByDataset = count.computeIfAbsent(variable, v -> new HashMap<>());
			for (Resource affectedDataset : datasets) {
				countByDataset.putIfAbsent(affectedDataset, 0);
				for (Map<String, Set<RDFNode>> valuesByVariable : valuesByVariableByResourceByDataset
						.getOrDefault(affectedDataset, Collections.emptyMap()).values()) {
					countByDataset.merge(affectedDataset,
							valuesByVariable.getOrDefault(variable, Collections.emptySet()).size(), Integer::sum);
				}
			}
		}
	}

	/**
	 * Calculates the count of values for the given datasets and values. Equivalent
	 * values per resource will be counted only once, but equivalent values per
	 * corresponding resource will be counted multiple times.
	 * 
	 * @param datasets
	 * @param variables
	 * @param valuesByVariableByResourceByDataset
	 */
	private void calculateCountDistinctPerResource(Set<Resource> datasets, Iterable<String> variables,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {

		ArrayList<RDFNode> distinctValues = new ArrayList<>();

		// init countDistinct
		countDistinct = new HashMap<>();

		for (String variable : variables) {
			var countDistinctByDataset = countDistinct.computeIfAbsent(variable, v -> new HashMap<>());
			for (Resource affectedDataset : datasets) {
				if (!valuesByVariableByResourceByDataset.containsKey(affectedDataset)) {
					continue;
				}
				var valuesByVariableByResource = valuesByVariableByResourceByDataset.get(affectedDataset);
				int countDistinctIntermediate = count.get(variable).get(affectedDataset);
				// determine distinct correction
				for (Map<String, Set<RDFNode>> valuesByVariable : valuesByVariableByResource.values()) {
					if (!valuesByVariable.containsKey(variable)) {
						continue;
					}
					distinctValues.clear();
					for (RDFNode value : valuesByVariable.get(variable)) {
						if (distinctValues.stream().anyMatch(v -> equivalentValues(v, value))) {
							countDistinctIntermediate--;
						} else {
							distinctValues.add(value);
						}
					}

				}
				// store measurement
				countDistinctByDataset.put(affectedDataset, countDistinctIntermediate);
			}
		}
	}

	private void updateCountDistinctForCorrespondingResources(Set<Resource> datasets, Iterable<String> variables,
			Map<Resource, Map<String, Map<RDFNode, Set<Resource>>>> resourcesByValueByVariableByDataset) {
		for (Resource affectedDataset : datasets) {
			if (!resourcesByValueByVariableByDataset.containsKey(affectedDataset)) {
				continue;
			}
			var resourcesByValueByVariable = resourcesByValueByVariableByDataset.get(affectedDataset);
			for (String variable : variables) {
				if (!resourcesByValueByVariable.containsKey(variable)) {
					continue;
				}
				var resourcesByValue = resourcesByValueByVariable.get(variable);
				int countAdjustment = 0;
				for (Set<Resource> resources : distinctByIdentity(resourcesByValue.values())) {
					countAdjustment = countAdjustment + 1 - resources.size();
				}
				countDistinct.get(variable).merge(affectedDataset, countAdjustment, Integer::sum);
			}
		}
	}

	private void calculateAbsoluteCoverage(String variable, Resource dataset1, Resource dataset2,
			Collection<Resource> resources1, Map<RDFNode, Set<Resource>> resourcesByMappedValues,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {
		int absoluteCoverageCount = 0;
		for (Resource resource1 : resources1) {
			for (RDFNode value1 : valuesByVariableByResourceByDataset.get(dataset1).get(resource1)
					.getOrDefault(variable, Collections.emptySet())) {
				if (// value not only from this dataset
				!resources1.containsAll(resourcesByMappedValues.getOrDefault(value1, Collections.emptySet()))) {
					absoluteCoverageCount++;
				}
			}
		}
		absoluteCoverage.computeIfAbsent(variable, v -> new HashMap<>()).computeIfAbsent(dataset2, v -> new HashMap<>())
				.merge(dataset1, absoluteCoverageCount, Integer::sum);
	}

	private <T> Set<T> distinctByIdentity(Collection<T> items) {
		Set<T> distinctItems = Collections.newSetFromMap(new IdentityHashMap<>());
		distinctItems.addAll(items);
		return distinctItems;
	}

	private void updatePairwiseOverlap(String variable, Collection<Resource> resources1,
			Collection<Resource> resources2, Map<RDFNode, Set<Resource>> resourcesByMappedValues) {
		int totalPairwiseOverlap = 0;
		// use set of resource sets for mapping values
		// NOTE: equivalent values use the same set, so get distinct set instances
		for (Set<Resource> resourceSet : distinctByIdentity(resourcesByMappedValues.values())) {
			if (!resources1.stream().anyMatch(resourceSet::contains)) {
				continue;
			}
			if (!resources2.stream().anyMatch(resourceSet::contains)) {
				continue;
			}
			totalPairwiseOverlap++;
		}

		totalPairwiseOverlapByVariable.merge(variable, totalPairwiseOverlap, Integer::sum);
	}

	private static <T> Set<T> intersection(Collection<T> a, Collection<T> b) {
		Set<T> set = new HashSet<>();
		set.addAll(a);
		set.retainAll(b);
		return set;
	}

	@Override
	public final void run() {
		Aspect aspect = this.getAspects().get(this.aspect);

		Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset = new HashMap<>();
		for (Resource dataset : aspect.getDatasets()) {
			valuesByVariableByResourceByDataset.put(dataset,
					Aspect.getResources(aspect, dataset, variables, this.getInputPrimaryModelUnion(dataset)));
		}

		calculateCount(aspect.getDatasets(), variables, valuesByVariableByResourceByDataset);
		calculateCountDistinctPerResource(aspect.getDatasets(), variables, valuesByVariableByResourceByDataset);
		getCorrespondenceGroups(aspect.getIri()).forEach(correspondingResources -> {
			var valuesPerDataset = getValuesPerDataset(correspondingResources, valuesByVariableByResourceByDataset);
			updateCountDistinctForCorrespondingResources(aspect.getDatasets(), variables, valuesPerDataset);
			for (Resource dataset1 : aspect.getDatasets()) {
				var resources1 = intersection(correspondingResources,
						valuesByVariableByResourceByDataset.get(dataset1).keySet());
				var valuesOfDataset1 = valuesPerDataset.get(dataset1);
				for (Resource dataset2 : aspect.getDatasets()) {
					if (dataset1.hashCode() < dataset2.hashCode()) {
						// do not do work twice
						continue;
					}
					var resources2 = intersection(correspondingResources,
							valuesByVariableByResourceByDataset.get(dataset2).keySet());
					var valuesOfDataset2 = valuesPerDataset.get(dataset2);
					for (String variable : variables) {
						this.compareVariableValues(variable, dataset1, resources1,
								valuesOfDataset1.getOrDefault(variable, Collections.emptyMap()), dataset2, resources2,
								valuesOfDataset2.getOrDefault(variable, Collections.emptyMap()),
								valuesByVariableByResourceByDataset);
					}
				}

			}
		});

		// calculate value population size per variable
		for (String variable : variables) {
			populationSize.put(variable, BigDecimal.ZERO);
			if (totalPairwiseOverlapByVariable.get(variable) != 0) {
				for (Resource affectedDataset : aspect.getDatasets()) {
					for (Resource comparedToDataset : aspect.getDatasets()) {
						if (affectedDataset.hashCode() >= comparedToDataset.hashCode()) {
							// only once per pair
							// do not use Resource#getURI() as it might be null for blank nodes
							continue;
						}
						populationSize.merge(variable,
								BigDecimal.valueOf(countDistinct.get(variable).get(affectedDataset)).multiply(
										BigDecimal.valueOf(countDistinct.get(variable).get(comparedToDataset))),
								BigDecimal::add);
					}
				}
				populationSize.merge(variable, BigDecimal.valueOf(totalPairwiseOverlapByVariable.get(variable)),
						(x, y) -> x.divide(y, SCALE, RoundingMode.HALF_UP));
			}
		}

		for (String variable : variables) {
			for (Resource affectedDataset : aspect.getDatasets()) {

				// calculate & store completeness:
				if (totalPairwiseOverlapByVariable.get(variable) != 0) {
					/** Ratio of values in an estimated population covered by this dataset */
					BigDecimal completeness = BigDecimal.valueOf(countDistinct.get(variable).get(affectedDataset))
							.divide(populationSize.get(variable), SCALE, RoundingMode.HALF_UP);
					Collection<Resource> otherDatasets = new HashSet<>(aspect.getDatasets());
					otherDatasets.remove(affectedDataset);
					Metadata.addQualityMeasurement(AV.marCompletenessThomas08, completeness, OM.one, affectedDataset,
							variable, otherDatasets, aspect.getIri(), this.getOutputMetaModel(affectedDataset));
				}

				// calculate relative coverage
				for (Resource comparedToDataset : aspect.getDatasets()) {
					if (affectedDataset.equals(comparedToDataset)) {
						continue;
					}
					int countComparedTo = count.get(variable).get(comparedToDataset);
					if (countComparedTo != 0) {
						int overlap = absoluteCoverage.getOrDefault(variable, Collections.emptyMap())
								.getOrDefault(affectedDataset, Collections.emptyMap())
								.getOrDefault(comparedToDataset, 0);
						relativeCoverage.computeIfAbsent(variable, v -> new HashMap<>())
								.computeIfAbsent(affectedDataset, v -> new HashMap<>())
								.put(comparedToDataset, BigDecimal.valueOf(overlap)
										.divide(BigDecimal.valueOf(countComparedTo), SCALE, RoundingMode.HALF_UP));
					}
				}
			}
		}

		// store measurements
		for (String variable : variables) {
			for (Resource affectedDataset : aspect.getDatasets()) {
				for (Resource comparedToDataset : aspect.getDatasets()) {
					if (!affectedDataset.equals(comparedToDataset)) {
						BigDecimal relativeCoverageValue = relativeCoverage
								.getOrDefault(variable, Collections.emptyMap())
								.getOrDefault(affectedDataset, Collections.emptyMap()).get(comparedToDataset);
						if (relativeCoverageValue != null) {
							Metadata.addQualityMeasurement(AV.relativeCoverage, relativeCoverageValue, OM.one,
									affectedDataset, variable, comparedToDataset, aspect.getIri(),
									this.getOutputMetaModel(affectedDataset));
						}
						Metadata.addQualityMeasurement(AV.absoluteCoverage,
								absoluteCoverage.getOrDefault(variable, Collections.emptyMap())
										.getOrDefault(affectedDataset, Collections.emptyMap())
										.getOrDefault(comparedToDataset, 0),
								OM.one, affectedDataset, variable, comparedToDataset, aspect.getIri(),
								this.getOutputMetaModel(affectedDataset));
					}
				}
				Metadata.addQualityMeasurement(AV.count, count.get(variable).get(affectedDataset), OM.one,
						affectedDataset, variable, aspect.getIri(), this.getOutputMetaModel(affectedDataset));
			}
		}
	}

	/**
	 * 
	 * Note: Not the most efficient way to do this, but there is no
	 * {@link Comparator} available to use {@link TreeMap#TreeMap(Comparator)}.
	 *
	 * @param resourcesByMappedValues
	 * @param values
	 */
	private void mapResources(Map<RDFNode, Set<Resource>> resourcesByMappedValues,
			Map<RDFNode, Set<Resource>> resourcesByValues) {
		for (RDFNode value : resourcesByValues.keySet()) {
			for (RDFNode valueKey : resourcesByMappedValues.keySet()) {
				if (equivalentValues(value, valueKey)) {
					Set<Resource> valuesSet = resourcesByMappedValues.get(valueKey);
					// add values to existing set
					valuesSet.addAll(resourcesByValues.get(value));
					// map value to same set, so equivalent values will share one set
					resourcesByMappedValues.putIfAbsent(value, valuesSet);
					break;
				}
			}
			// no equivalent value in map
			resourcesByMappedValues.computeIfAbsent(value, v -> new HashSet<>()).addAll(resourcesByValues.get(value));
		}
	}

	/**
	 * Compares the values of one variable from two corresponding resources in two
	 * datasets and stores encountered deviations and issues in both according
	 * outputMetaModels (derived by {@link #getOutputMetaModel(Resource)}). Either
	 * the corresponding resources or the datasets might be equal, but not both at
	 * once.
	 * 
	 * @param variable               Name of the compared variable
	 * @param dataset1               IRI of the first dataset
	 * @param correspondingResource1 IRI of the first resource
	 * @param resourcesByValue1      Variable values of the first resource
	 * @param dataset2               IRI of the second dataset
	 * @param resourcesByValue2      Variable values of the second resource
	 */
	public void compareVariableValues(String variable, Resource dataset1, Set<Resource> resources1,
			Map<RDFNode, Set<Resource>> resourcesByValue1, Resource dataset2, Set<Resource> resources2,
			Map<RDFNode, Set<Resource>> resourcesByValue2,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {

		Map<RDFNode, Set<Resource>> resourcesByMappedValues = new HashMap<>();
		mapResources(resourcesByMappedValues, resourcesByValue1);
		mapResources(resourcesByMappedValues, resourcesByValue2);

		// deviation: pair of resources with each having a value not present in the
		// other resource
		// omission: pair of resources with one having a value not present in the other,
		// but not vice versa
		for (Resource resource1 : resources1) {
			var values1 = valuesByVariableByResourceByDataset.get(dataset1).get(resource1)
					.getOrDefault(variable, Collections.emptySet()).stream().filter(v -> isValidValue(v))
					.collect(Collectors.toSet());
			for (Resource resource2 : resources2) {
				var values2 = valuesByVariableByResourceByDataset.get(dataset2).get(resource2)
						.getOrDefault(variable, Collections.emptySet()).stream().filter(v -> isValidValue(v))
						.collect(Collectors.toSet());
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

		// update measurements
		calculateAbsoluteCoverage(variable, dataset1, dataset2, resources1, resourcesByMappedValues,
				valuesByVariableByResourceByDataset);
		calculateAbsoluteCoverage(variable, dataset2, dataset1, resources2, resourcesByMappedValues,
				valuesByVariableByResourceByDataset);
		if (!dataset1.equals(dataset2)) {
			updatePairwiseOverlap(variable, resources1, resources2, resourcesByMappedValues);
		}
	}

	/**
	 * Checks if a value is valid.
	 * 
	 * @param variable the variable the value belongs to
	 * @param dataset  the dataset the value belongs to
	 * @param resource the resource the value belongs to
	 * @param value    the value to check
	 * @return {@code true}, if the value is valid, otherwise {@code false}
	 */
	public abstract boolean isValidValue(RDFNode value);

	public abstract String invalidValueComment();

	/**
	 * Checks if two values are equivalent.
	 * 
	 * @param variable the variable the value belongs to
	 * @param dataset  the dataset the value belongs to
	 * @param resource the resource the value belongs to
	 * @param value    the value to check
	 * @return {@code true}, if the value is valid, otherwise {@code false}
	 */
	public abstract boolean equivalentValues(RDFNode value1, RDFNode value2);

	/**
	 * Checks if a valid value should be used for comparison.
	 * 
	 * @param value the value to check
	 * @return {@code true}, if the value should be used, otherwise {@code false}
	 */
	public boolean useValue(RDFNode value) {
		return true;
	}
}
