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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

/**
 * Provides measurements for <strong>number of resources</strong>,
 * <strong>absolute coverage</strong>, <strong>relative coverage</strong>, and
 * <strong>completeness</strong> per aspect (estimated using the mark and
 * recapture method as defined in
 * <a href="https://doi.org/10.1145/1390334.1390531">Thomas 2008</a>), as well
 * as <strong>deviation</strong>, <strong>resource omission</strong> and
 * <strong>duplicate</strong> annotations.
 */
public class PopulationComparisonProcessor extends Processor<PopulationComparisonProcessor> {

	/**
	 * Digits to preserve when rounding after division in measurement calculations.
	 */
	public final static int SCALE = 16;

	/**
	 * The {@link Aspect Aspects} to process.
	 */
	@Parameter
	public Collection<Resource> aspects;

	@Override
	public void run() {
		for (Resource aspectIri : aspects) {

			// Number of covered resources of another dataset, excluding duplicates.
			Map<Resource, Map<Resource, Integer>> absoluteCoverage = new HashMap<>();
			// Number of resources in this dataset.
			Map<Resource, Integer> count = new HashMap<>();
			// Number of resources in this dataset, excluding duplicates.
			Map<Resource, Integer> deduplicatedCount = new HashMap<>();

			Aspect aspect = this.getAspects().get(aspectIri);

			Set<Resource> datasetsCoveringTheAspect = aspect.getDatasets();

			// get resources by dataset and aspect
			Map<Resource, Set<Resource>> uncoveredResourcesByDataset = new HashMap<>();
			for (Resource dataset : datasetsCoveringTheAspect) {
				uncoveredResourcesByDataset.put(dataset,
						Aspect.getResourceKeys(aspect, dataset, this.getInputPrimaryModelUnion(dataset))
								.collect(Collectors.toSet()));
				// store count and initial deduplicated count
				count.put(dataset, uncoveredResourcesByDataset.get(dataset).size());
				deduplicatedCount.put(dataset, uncoveredResourcesByDataset.get(dataset).size());
			}

			// prepare measurements
			for (Resource dataset : datasetsCoveringTheAspect) {
				for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
					absoluteCoverage.computeIfAbsent(dataset, x -> new HashMap<>()).put(datasetComparedTo, 0);
				}
			}

			// process correspondence sets of aspect
			getCorrespondenceGroups(aspectIri).forEach(correspondingResources -> {
				Map<Resource, Set<Resource>> occurrencesByDataset = new HashMap<>();
				// count resources of the dataset in the correspondence set
				for (Resource dataset : datasetsCoveringTheAspect) {
					Set<Resource> uncoveredResourcesOfDataset = uncoveredResourcesByDataset.get(dataset);
					Set<Resource> occurrencesOfDataset = occurrencesByDataset.computeIfAbsent(dataset,
							r -> new HashSet<>());
					for (Resource correspondingResource : correspondingResources) {
						if (uncoveredResourcesOfDataset.contains(correspondingResource)) {
							occurrencesOfDataset.add(correspondingResource);
						}
					}
				}
				for (Resource dataset : datasetsCoveringTheAspect) {
					int occurrences = occurrencesByDataset.containsKey(dataset)
							? occurrencesByDataset.get(dataset).size()
							: 0;
					if (occurrences == 0) {
						// report resource omission for resources in correspondence sets
						for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
							for (Resource resourceComparedTo : occurrencesByDataset.get(datasetComparedTo)) {
								Metadata.addResourceOmission(dataset, datasetComparedTo, resourceComparedTo,
										aspect.getIri(), this.getOutputMetaModel(dataset));
							}
						}
					}
					if (occurrences > 0) {
						for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
							if (!occurrencesByDataset.get(datasetComparedTo).isEmpty()) {
								// do not use Resource#getURI() as it might be null for blank nodes
								if (dataset.hashCode() < datasetComparedTo.hashCode()) { // only once per pair

									// count covered resources of the compared dataset (both directions)
									absoluteCoverage.get(dataset).merge(datasetComparedTo, 1, Integer::sum);
									absoluteCoverage.get(datasetComparedTo).merge(dataset, 1, Integer::sum);
								}
							}
						}
						if (occurrences > 1) {
							// update deduplicated count: subtract duplicates excluding the one to stay
							deduplicatedCount.merge(dataset, 1-occurrences, Integer::sum);
							// report duplicates
							for (Resource duplicateResource1 : occurrencesByDataset.get(dataset)) {
								for (Resource duplicateResource2 : occurrencesByDataset.get(dataset)) {
									if (!duplicateResource1.equals(duplicateResource2)) {
										Metadata.addIssue(duplicateResource1, null, null, aspectIri,
												"Duplicated Resource", "of <" + duplicateResource2 + ">",
												this.getOutputMetaModel(dataset));
									}
								}
							}

						}
					}
				}

				// remove covered resources
				for (Resource dataset : datasetsCoveringTheAspect) {
					correspondingResources.forEach(uncoveredResourcesByDataset.get(dataset)::remove);
				}
			});

			int totalPairwiseOverlap = 0;
			for (Resource dataset : datasetsCoveringTheAspect) {
				for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
					if (dataset.hashCode() < datasetComparedTo.hashCode()) { // only once per pair
						totalPairwiseOverlap += absoluteCoverage.get(dataset).get(datasetComparedTo);
					}
				}
			}

			if (totalPairwiseOverlap != 0) {
				// calculate estimated population size
				BigDecimal populationSize = BigDecimal.ZERO;
				for (Resource dataset : datasetsCoveringTheAspect) {
					for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
						// do not use Resource#getURI() as it might be null for blank nodes
						if (dataset.hashCode() < datasetComparedTo.hashCode()) {
							populationSize = populationSize.add(BigDecimal.valueOf(deduplicatedCount.get(dataset))
									.multiply(BigDecimal.valueOf(deduplicatedCount.get(datasetComparedTo))));
						}
					}
				}
				populationSize = populationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap), 0,
						RoundingMode.HALF_UP);

				// calculate & store estimated population completeness
				for (Resource dataset : datasetsCoveringTheAspect) {

					// ratio of resources in an estimated population covered by this dataset
					BigDecimal completeness = BigDecimal.valueOf(deduplicatedCount.get(dataset)).divide(populationSize, SCALE,
							RoundingMode.HALF_UP);
					Collection<Resource> otherDatasets = new HashSet<>(datasetsCoveringTheAspect);
					otherDatasets.remove(dataset);
					Metadata.addQualityMeasurement(AV.marCompletenessThomas08, completeness, OM.one, dataset,
							otherDatasets, aspect.getIri(), this.getOutputMetaModel(dataset));
				}
			}

			// measures
			for (Resource dataset : datasetsCoveringTheAspect) {
				// store count
				Metadata.addQualityMeasurement(AV.count, count.get(dataset), OM.one, dataset, aspect.getIri(),
						this.getOutputMetaModel(dataset));
				// store deduplicated count
				Metadata.addQualityMeasurement(AV.deduplicatedCount, deduplicatedCount.get(dataset), OM.one, dataset, aspect.getIri(),
						this.getOutputMetaModel(dataset));
				for (Resource datasetComparedTo : datasetsCoveringTheAspect) {
					if (!dataset.equals(datasetComparedTo)) {
						// store absolute coverage
						if (absoluteCoverage.get(dataset).get(datasetComparedTo) != null) {
							Metadata.addQualityMeasurement(AV.absoluteCoverage,
									absoluteCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset,
									datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
						}
						// calculate & store relative coverage
						int countComparedTo = deduplicatedCount.get(datasetComparedTo);
						if (countComparedTo != 0) {
							int overlap = absoluteCoverage.get(dataset).get(datasetComparedTo);
							Metadata.addQualityMeasurement(AV.relativeCoverage,
									BigDecimal.valueOf(overlap).divide(BigDecimal.valueOf(countComparedTo),
											SCALE, RoundingMode.HALF_UP), OM.one, dataset,
									datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
						}
						// report resource omissions for resources not in correspondence sets
						for (Resource uncoveredResource : uncoveredResourcesByDataset.get(datasetComparedTo)) {
							Metadata.addResourceOmission(dataset, datasetComparedTo, uncoveredResource, aspect.getIri(),
									this.getOutputMetaModel(dataset));
						}
					}
				}
			}
		}
	}
}
