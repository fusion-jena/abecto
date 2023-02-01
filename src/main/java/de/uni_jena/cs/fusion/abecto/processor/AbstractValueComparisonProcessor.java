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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
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
	private Map<String, Map<Resource, Integer>> count = new HashMap<>();
	/**
	 * Number of distinct values in this dataset, per variable.
	 * 
	 * Index: variable, affectedDataset
	 */
	private Map<String, Map<Resource, Integer>> countDistinct = new HashMap<>();
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
	 * Returns the number of given non equivalent values.
	 * 
	 * @param values
	 * @return
	 */
	private int countDistinct(Iterable<RDFNode> values) {
		ArrayList<RDFNode> distinctValues = new ArrayList<>();
		for (RDFNode value : values) {
			if (!distinctValues.stream().anyMatch(v -> equivalentValues(v, value))) {
				distinctValues.add(value);
			}
		}
		return distinctValues.size();
	}

	private void calculateAbsoluteCoverage(String variable, Resource dataset1, Resource dataset2,
			Collection<Resource> resources1, Map<RDFNode, Set<Resource>> resourcesByMappedValues,
			Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1) {
		int absoluteCoverageCount = 0;
		for (Resource resource1 : resources1) {
			for (RDFNode value1 : valuesByVariableByResource1.get(resource1).getOrDefault(variable,
					Collections.emptySet())) {
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

	private void updateTotalPairwiseOverlap(String variable, Collection<Resource> resources1,
			Collection<Resource> resources2, Map<RDFNode, Set<Resource>> resourcesByMappedValues) {
		int pairwiseOverlap = 0;
		// use set of resource sets for mapping values
		// NOTE: equivalent values use the same set, so get distinct set instances
		for (Set<Resource> resourceSet : distinctByIdentity(resourcesByMappedValues.values())) {
			if (!resources1.stream().anyMatch(resourceSet::contains)) {
				continue;
			}
			if (!resources2.stream().anyMatch(resourceSet::contains)) {
				continue;
			}
			pairwiseOverlap++;
		}

		totalPairwiseOverlapByVariable.merge(variable, pairwiseOverlap, Integer::sum);
	}

	/**
	 * Removes all values that do not match to {@link #isValidValue(RDFNode)},
	 * {@link #useValue(RDFNode)}, or are known wrong values.
	 * 
	 * @param valuesByVariableByResource
	 * @param dataset
	 */
	private void filterValues(Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource, Resource dataset) {
		for (Resource resource : valuesByVariableByResource.keySet()) {
			filterValues(valuesByVariableByResource.get(resource), resource, dataset);
		}
	}

	/**
	 * Removes all values that do not match to {@link #isValidValue(RDFNode)},
	 * {@link #useValue(RDFNode)}, or are known wrong values.
	 * 
	 * @param valuesByVariableByResource
	 * @param resource
	 * @param dataset
	 */
	private void filterValues(Map<String, Set<RDFNode>> valuesByVariable, Resource resource, Resource dataset) {
		Model inputMetaModel = this.getInputMetaModelUnion(dataset);
		for (String variable : valuesByVariable.keySet()) {
			Set<RDFNode> values = valuesByVariable.get(variable);
			values.removeIf(value -> !isValidValue(value));
			values.removeIf(value -> !useValue(value));
			values.removeIf(value -> Metadata.isWrongValue(resource, variable, value, aspect, inputMetaModel));
		}
	}

	/**
	 * Store an invalid value issue for each value that does not match to
	 * {@link #isValidValue(RDFNode)} in the according output meta model.
	 * 
	 * @param valuesByVariableByResource
	 * @param dataset
	 */
	private void reportInvalidValues(Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource,
			Resource dataset) {
		for (Resource resource : valuesByVariableByResource.keySet()) {
			Map<String, Set<RDFNode>> valuesByVariable = valuesByVariableByResource.get(resource);
			for (String variable : valuesByVariable.keySet()) {
				Iterator<RDFNode> values = valuesByVariable.get(variable).iterator();
				while (values.hasNext()) {
					RDFNode value = values.next();
					if (!this.isValidValue(value)) {
						// report invalid value
						Metadata.addIssue(resource, variable, value, aspect, "Invalid Value",
								this.invalidValueComment(), this.getOutputMetaModel(dataset));
					}
				}
			}
		}
	}

	@Override
	public final void run() {
		Aspect aspect = this.getAspects().get(this.aspect);

		// init count and countDistinct
		for (String variable : variables) {
			this.count.put(variable, new HashMap<>());
			this.countDistinct.put(variable, new HashMap<>());
		}

		for (Resource dataset : aspect.getDatasets()) {
			Model model = this.getInputPrimaryModelUnion(dataset);

			Aspect.getResourceKeys(aspect, dataset, model).forEach(resource -> {
				// get resource values
				Map<String, Set<RDFNode>> valuesByVariable = aspect.selectResourceValues(resource, dataset, variables,
						model);

				for (String variable : variables) {
					// measure count of values
					this.count.get(variable).merge(dataset,
							valuesByVariable.getOrDefault(variable, Collections.emptySet()).size(), Integer::sum);

					// measure count of non equivalent values per resource
					this.countDistinct.get(variable).merge(dataset,
							countDistinct(valuesByVariable.getOrDefault(variable, Collections.emptySet())),
							Integer::sum);
				}

			});
		}

		getCorrespondenceGroups(aspect.getIri()).forEach(correspondingResources -> {
			for (Resource dataset1 : aspect.getDatasets()) {
				Model model1 = this.getInputPrimaryModelUnion(dataset1);
				Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1 = aspect
						.selectResourceValues(correspondingResources, dataset1, variables, model1);
				reportInvalidValues(valuesByVariableByResource1, dataset1);
				filterValues(valuesByVariableByResource1, dataset1);

				for (String variable : variables) {
					// get values of all corresponding resources
					var valuesOfCorrespondingResources = new ArrayList<RDFNode>();
					valuesByVariableByResource1.values().stream()
							.map(m -> m.getOrDefault(variable, Collections.emptySet()))
							.forEach(valuesOfCorrespondingResources::addAll);

					// measure count of non equivalent values per corresponding resources
					this.countDistinct.get(variable).merge(dataset1,
							countDistinct(valuesOfCorrespondingResources) - valuesOfCorrespondingResources.size(),
							Integer::sum);
				}

				for (Resource dataset2 : aspect.getDatasets()) {
					if (dataset1.hashCode() < dataset2.hashCode()) {
						// do not do work twice
						continue;
					}
					Model model2 = this.getInputPrimaryModelUnion(dataset2);
					Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource2 = aspect
							.selectResourceValues(correspondingResources, dataset2, variables, model2);
					filterValues(valuesByVariableByResource2, dataset2);
					for (String variable : variables) {
						this.compareVariableValues(variable, dataset1, valuesByVariableByResource1, dataset2,
								valuesByVariableByResource2);
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
					int countComparedTo = count.getOrDefault(variable, Collections.emptyMap())
							.getOrDefault(comparedToDataset, 0);
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
				Metadata.addQualityMeasurement(AV.count, count.get(variable).getOrDefault(affectedDataset, 0), OM.one,
						affectedDataset, variable, aspect.getIri(), this.getOutputMetaModel(affectedDataset));
			}
		}
	}

	/**
	 * 
	 * Note: Not the most efficient way to do this, but there is no
	 * {@link Comparator} available to use {@link TreeMap#TreeMap(Comparator)}.
	 *
	 * @param valuesByVariableByResource
	 * @param values
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
	 * @param correspondingResource1      IRI of the first datasets resources
	 * @param valuesByVariableByResource1 Values of the first datasets resources
	 * @param dataset2                    IRI of the second datasets dataset
	 * @param correspondingResource1      IRI of the second resources
	 * @param valuesByVariableByResource2 Values of the second datasets resources
	 */
	public void compareVariableValues(String variable, Resource dataset1,
			Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1, Resource dataset2,
			Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource2) {

		Map<RDFNode, Set<Resource>> resourcesByMappedValues = new HashMap<>();
		mapResources(variable, resourcesByMappedValues, valuesByVariableByResource1);
		mapResources(variable, resourcesByMappedValues, valuesByVariableByResource2);

		// deviation: pair of resources with each having a value not present in the
		// other resource
		// omission: pair of resources with one having a value not present in the other,
		// but not vice versa
		for (Resource resource1 : valuesByVariableByResource1.keySet()) {
			var values1 = valuesByVariableByResource1.get(resource1).getOrDefault(variable, Collections.emptySet())
					.stream().filter(v -> isValidValue(v)).collect(Collectors.toSet());
			for (Resource resource2 : valuesByVariableByResource2.keySet()) {
				var values2 = valuesByVariableByResource2.get(resource2).getOrDefault(variable, Collections.emptySet())
						.stream().filter(v -> isValidValue(v)).collect(Collectors.toSet());
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
		calculateAbsoluteCoverage(variable, dataset1, dataset2, valuesByVariableByResource1.keySet(),
				resourcesByMappedValues, valuesByVariableByResource1);
		calculateAbsoluteCoverage(variable, dataset2, dataset1, valuesByVariableByResource2.keySet(),
				resourcesByMappedValues, valuesByVariableByResource2);
		if (!dataset1.equals(dataset2)) {
			updateTotalPairwiseOverlap(variable, valuesByVariableByResource1.keySet(),
					valuesByVariableByResource2.keySet(), resourcesByMappedValues);
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
	 * @param value1 the first value to compare
	 * @param value2 the second value to compare
	 * @return {@code true}, if the values are equivalent, otherwise {@code false}
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
