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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 * Number of overlaps between all pairs of dataset by dataset compared to by
	 * multiplied by 2.
	 */
	private Map<String, Integer> totalPairwiseOverlapTwice = new HashMap<>();
	/**
	 * Estimated population site of values, per variable.
	 */
	private Map<String, BigDecimal> populationSize = new HashMap<>();

	/**
	 * Returns a merge per dataset of all values of the given resources. Values that
	 * are invalid due to {@link #isValidValue(RDFNode)}, should not be used due to
	 * {@link #useValue(RDFNode)} ignored due to Invalid or are known to be wrong
	 * will not be added.
	 * 
	 * @return merge of the vaules per dataset; index: dataset, variable, value,
	 *         resource
	 */
	private Map<Resource, Map<String, Map<RDFNode, Collection<Resource>>>> getValuesPerDataset(
			Collection<Resource> resources,
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {
		// init result map
		Map<Resource, Map<String, Map<RDFNode, Collection<Resource>>>> datasetValueMerges = new HashMap<>();
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

	private void calculateCounts(
			Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {
		// calculate count
		for (String variable : variables) {
			// init count
			Map<Resource, Integer> countByDataset = count.computeIfAbsent(variable, v -> new HashMap<>());
			for (Resource affectedDataset : valuesByVariableByResourceByDataset.keySet()) {
				for (Map<String, Set<RDFNode>> valuesByVariable : valuesByVariableByResourceByDataset
						.get(affectedDataset).values()) {
					// update count
					countByDataset.merge(affectedDataset,
							valuesByVariable.getOrDefault(variable, Collections.emptySet()).size(), Integer::sum);
				}
			}
		}
	}

	@Override
	public final void run() {
		Aspect aspect = this.getAspects().get(this.aspect);

		/** dataset -> resource -> variable -> values */
		Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset = new HashMap<>();
		for (Resource dataset : aspect.getDatasets()) {
			valuesByVariableByResourceByDataset.put(dataset,
					Aspect.getResources(aspect, dataset, variables, this.getInputPrimaryModelUnion(dataset)));
		}

		calculateCounts(valuesByVariableByResourceByDataset);

		getCorrespondenceGroups(aspect.getIri()).forEach(correspondingResources -> {
			var valuesPerDataset = getValuesPerDataset(correspondingResources, valuesByVariableByResourceByDataset);
			for (Resource dataset1 : aspect.getDatasets()) {
				var resources1 = valuesByVariableByResourceByDataset.get(dataset1).keySet();
				var valuesOfDataset1 = valuesPerDataset.get(dataset1);
				for (Resource dataset2 : aspect.getDatasets()) {
					if (dataset1.hashCode() < dataset2.hashCode()) {
						// do not do work twice
						continue;
					}
					var resources2 = valuesByVariableByResourceByDataset.get(dataset2).keySet();
					var valuesOfDataset2 = valuesPerDataset.get(dataset2);
					for (String variable : variables) {
						this.compareVariableValues(variable, dataset1, resources1,
								valuesOfDataset1.getOrDefault(variable, Collections.emptyMap()), dataset2, resources2,
								valuesOfDataset2.getOrDefault(variable, Collections.emptyMap()));
					}
				}

			}
		});

		// calculate value population size per variable
		for (String variable : variables) {
			populationSize.put(variable, BigDecimal.ZERO);
			if (totalPairwiseOverlapTwice.get(variable) != 0) {
				for (Resource affectedDataset : aspect.getDatasets()) {
					for (Resource comparedToDataset : aspect.getDatasets()) {
						if (affectedDataset.hashCode() >= comparedToDataset.hashCode()) {
							// only once per pair
							// do not use Resource#getURI() as it might be null for blank nodes
							continue;
						}
						populationSize.merge(variable,
								BigDecimal.valueOf(count.get(variable).get(affectedDataset))
										.multiply(BigDecimal.valueOf(count.get(variable).get(comparedToDataset))),
								BigDecimal::add);
					}
				}
				// TODO rounding mode ceiling right?
				populationSize.merge(variable,
						BigDecimal.valueOf(totalPairwiseOverlapTwice.get(variable)).divide(BigDecimal.valueOf(2),0,RoundingMode.CEILING),
						(x, y) -> x.divide(y, 0, RoundingMode.HALF_UP));
			}
		}

		for (String variable : variables) {
			for (Resource affectedDataset : aspect.getDatasets()) {

				// calculate & store completeness:
				if (totalPairwiseOverlapTwice.get(variable) != 0) {
					/** Ratio of values in an estimated population covered by this dataset */
					BigDecimal completeness = BigDecimal.valueOf(count.get(variable).get(affectedDataset))
							.divide(populationSize.get(variable), 2, RoundingMode.HALF_UP);
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
										.divide(BigDecimal.valueOf(countComparedTo), 2, RoundingMode.HALF_UP));
					}
				}
			}
		}

		// store measurements
		for (String variable : variables) {
			for (Resource affectedDataset : aspect.getDatasets()) {
				for (Resource comparedToDataset : aspect.getDatasets()) {
					if (!affectedDataset.equals(comparedToDataset)) {
						Metadata.addQualityMeasurement(AV.relativeCoverage,
								relativeCoverage.getOrDefault(variable, Collections.emptyMap())
										.getOrDefault(affectedDataset, Collections.emptyMap())
										.getOrDefault(comparedToDataset, BigDecimal.ZERO),
								OM.one, affectedDataset, variable, comparedToDataset, aspect.getIri(),
								this.getOutputMetaModel(affectedDataset));
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
	 * Compares the values of one variable from two corresponding resources in two
	 * datasets and stores encountered deviations and issues in both according
	 * outputMetaModels (derived by {@link #getOutputMetaModel(Resource)}). Either
	 * the corresponding resources or the datasets might be equal, but not both at
	 * once.
	 * 
	 * @param variable               Name of the compared variable
	 * @param dataset1               IRI of the first dataset
	 * @param correspondingResource1 IRI of the first resource
	 * @param values1                Variable values of the first resource
	 * @param dataset2               IRI of the second dataset
	 * @param values2                Variable values of the second resource
	 */
	public void compareVariableValues(String variable, Resource dataset1, Collection<Resource> resources1,
			Map<RDFNode, Collection<Resource>> values1, Resource dataset2, Collection<Resource> resources2,
			Map<RDFNode, Collection<Resource>> values2) {

		var notMatchingValues1 = new HashSet<>(values1.keySet());
		var notMatchingValues2 = new HashSet<>(values2.keySet());

		var matchingValues1 = new HashSet<RDFNode>();
		var matchingValues2 = new HashSet<RDFNode>();

		// find matching values
		for (RDFNode value1 : values1.keySet()) {
			for (RDFNode value2 : values2.keySet()) {
				if (equivalentValues(value1, value2)) {
					notMatchingValues1.remove(value1);
					notMatchingValues2.remove(value2);
					matchingValues1.add(value1);
					matchingValues2.add(value2);
					// NOTE: do not break loop, there might be cases of multiple equivalent values
				}
			}
		}

		// report missing matching values
		for (Resource resource1 : resources1) {
			nextValue: for (RDFNode value2 : matchingValues2) {
				for (RDFNode value1 : values1.keySet()) {
					if (values1.get(value1).contains(resource1) && equivalentValues(value1, value2)) {
						continue nextValue;
					}
				}
				for (Resource resource2 : values2.get(value2)) {
					Metadata.addValuesOmission(resource1.asResource(), variable, dataset2, resource2.asResource(),
							value2, this.aspect, this.getOutputMetaModel(dataset1));
				}
			}
		}
		for (Resource resource2 : resources2) {
			nextValue: for (RDFNode value1 : matchingValues1) {
				for (RDFNode value2 : values2.keySet()) {
					if (values2.get(value2).contains(resource2) && equivalentValues(value2, value1)) {
						continue nextValue;
					}
				}
				for (Resource resource1 : values1.get(value1)) {
					Metadata.addValuesOmission(resource2.asResource(), variable, dataset1, resource1.asResource(),
							value1, this.aspect, this.getOutputMetaModel(dataset2));
				}
			}
		}

		// report missing not matching values
		if (notMatchingValues1.isEmpty()) {
			for (RDFNode value2 : notMatchingValues2) {
				for (Resource resource2 : values2.get(value2)) {
					for (Resource resource1 : resources1) {
						Metadata.addValuesOmission(resource1, variable, dataset2, resource2, value2, this.aspect,
								this.getOutputMetaModel(dataset1));
					}
				}
			}
		} else if (notMatchingValues2.isEmpty()) {
			for (RDFNode value1 : notMatchingValues1) {
				for (Resource resource1 : values1.get(value1)) {
					for (Resource resource2 : resources2) {
						Metadata.addValuesOmission(resource2, variable, dataset1, resource1, value1, this.aspect,
								this.getOutputMetaModel(dataset2));
					}
				}
			}
		} else {
			// report pairs of deviating values
			for (RDFNode value1 : notMatchingValues1) {
				for (RDFNode value2 : notMatchingValues2) {
					for (Resource resource1 : values1.get(value1)) {
						for (Resource resource2 : values2.get(value2)) {
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
		if (!dataset1.equals(dataset2)) {
			int overlapTwice = 0;
			for (RDFNode value1 : matchingValues1) {
				overlapTwice += values1.get(value1).size();
				absoluteCoverage.computeIfAbsent(variable, v -> new HashMap<>())
						.computeIfAbsent(dataset2, v -> new HashMap<>())
						.merge(dataset1, values1.getOrDefault(value1, Collections.emptySet()).size(), Integer::sum);
			}
			for (RDFNode value2 : matchingValues2) {
				overlapTwice += values2.get(value2).size();
				absoluteCoverage.computeIfAbsent(variable, v -> new HashMap<>())
						.computeIfAbsent(dataset1, v -> new HashMap<>())
						.merge(dataset2, values2.getOrDefault(value2, Collections.emptySet()).size(), Integer::sum);
			}
			// NOTE: mark and recapture assumes equal absoluteCoverage in both directions,
			// use average (floor) to approximate
			totalPairwiseOverlapTwice.merge(variable, overlapTwice, Integer::sum);
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
