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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

/**
 * Provides measurements for <strong>number of duplicates</strong>,
 * <strong>absolute coverage</strong>, <strong>relative coverage</strong>, and
 * <strong>completeness</strong> per aspect (estimated using the mark and
 * recapture method as defined in defined in
 * <a href="https://doi.org/10.1145/1390334.1390531">Thomas 2008</a>), as well
 * as <strong>resource omission</strong> and <strong>duplicate</strong>
 * annotations.
 */
public class CompletenessProcessor extends Processor<CompletenessProcessor> {
	final static Logger log = LoggerFactory.getLogger(CompletenessProcessor.class);

	/**
	 * The {@link Aspect Aspects} to process.
	 */
	@Parameter
	public Collection<Resource> aspects;

	@Override
	public void run() {
		for (Resource aspectIri : aspects) {

			/** Number of covered resources of another dataset, excluding duplicates. */
			Map<Resource, Map<Resource, Integer>> absoluteCoverage = new HashMap<>();
			/** Ratio of covered resources of another dataset, excluding duplicates. */
			Map<Resource, Map<Resource, BigDecimal>> relativeCoverage = new HashMap<>();
			/** Number of duplicate resources in this dataset, excluding the on to stay. */
			Map<Resource, Integer> duplicates = new HashMap<>();
			/** Number of resources in this dataset, excluding duplicates. */
			Map<Resource, Integer> count = new HashMap<>();
			/** Number of resources not covered by this dataset. */
			Map<Resource, Integer> omissions = new HashMap<>();
			/** Number of overlaps between all pairs of dataset, excluding duplicates. */
			AtomicInteger totalPairwiseOverlap = new AtomicInteger(0);

			Aspect aspect = this.getAspects().get(aspectIri);

			Set<Resource> datasetsCoveringTheAspekt = aspect.getDatasets();

			// get resources by dataset and aspect
			Map<Resource, Set<Resource>> uncoveredResourcesByDataset = new HashMap<>();
			for (Resource dataset : datasetsCoveringTheAspekt) {
				uncoveredResourcesByDataset.put(dataset,
						Aspect.getResourceKeys(aspect, dataset, this.getInputPrimaryModelUnion(dataset)));
				// store count
				count.put(dataset, uncoveredResourcesByDataset.get(dataset).size());
			}

			// prepare measurements
			for (Resource dataset : datasetsCoveringTheAspekt) {
				for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
					absoluteCoverage.computeIfAbsent(dataset, x -> new HashMap<>()).put(datasetComparedTo, 0);
				}
			}

			// process correspondence sets of aspect
			getCorrespondenceGroups(aspectIri).forEach(correspondingResources -> {
				Map<Resource, Set<Resource>> occurrencesByDataset = new HashMap<>();
				// count resources of the dataset in the correspondence set
				for (Resource dataset : datasetsCoveringTheAspekt) {
					Set<Resource> uncoveredResourcesOfDataset = uncoveredResourcesByDataset.get(dataset);
					Set<Resource> occurrencesOfDataset = occurrencesByDataset.computeIfAbsent(dataset,
							r -> new HashSet<>());
					for (Resource correspondingResource : correspondingResources) {
						if (uncoveredResourcesOfDataset.contains(correspondingResource)) {
							occurrencesOfDataset.add(correspondingResource);
						}
					}
				}
				for (Resource dataset : datasetsCoveringTheAspekt) {
					int occurrences = occurrencesByDataset.containsKey(dataset)
							? occurrencesByDataset.get(dataset).size()
							: 0;
					if (occurrences == 0) {
						// count correspondence sets the dataset is not participating in
						omissions.merge(dataset, 1, Integer::sum);

						// report resource omission for resources in correspondence sets
						for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
							for (Resource resourceComparedTo : occurrencesByDataset.get(datasetComparedTo)) {
								Metadata.addResourceOmission(dataset, datasetComparedTo, resourceComparedTo,
										aspect.getIri(), this.getOutputMetaModel(dataset));
							}
						}
					}
					if (occurrences > 0) {
						for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
							if (!occurrencesByDataset.get(datasetComparedTo).isEmpty()) {
								// do not use Resource#getURI() as it might be null for blank nodes
								if (dataset.hashCode() < datasetComparedTo.hashCode()) {
									// only once per pair

									// count covered resources of the compared dataset (both directions)
									absoluteCoverage.get(dataset).merge(datasetComparedTo, 1, Integer::sum);
									absoluteCoverage.get(datasetComparedTo).merge(dataset, 1, Integer::sum);

									// count total pairwise overlap
									totalPairwiseOverlap.incrementAndGet();
								}
							}
						}
						if (occurrences > 1) {
							// count duplicates, excluding the one to stay
							duplicates.merge(dataset, occurrences - 1, Integer::sum);

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
				for (Resource dataset : datasetsCoveringTheAspekt) {
					uncoveredResourcesByDataset.get(dataset).removeAll(correspondingResources);
				}
			});

			// report resource omissions for resources not in correspondence sets
			for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
				for (Resource dataset : datasetsCoveringTheAspekt) {
					if (!dataset.equals(datasetComparedTo)) {
						for (Resource uncoveredResource : uncoveredResourcesByDataset.get(datasetComparedTo)) {
							Metadata.addResourceOmission(dataset, datasetComparedTo, uncoveredResource, aspect.getIri(),
									this.getOutputMetaModel(dataset));
						}
					}
				}
			}

			// adjust and store resource count by duplicates count
			for (Resource dataset : datasetsCoveringTheAspekt) {
				count.merge(dataset, -duplicates.getOrDefault(dataset, 0), Integer::sum);

				Metadata.addQualityMeasurement(AV.count, count.get(dataset), OM.one, dataset, aspect.getIri(),
						this.getOutputMetaModel(dataset));
			}

			// calculate population size
			BigDecimal populationSize = BigDecimal.ZERO;
			if (totalPairwiseOverlap.get() != 0) {
				for (Resource dataset : datasetsCoveringTheAspekt) {
					for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
						// do not use Resource#getURI() as it might be null for blank nodes
						if (dataset.hashCode() < datasetComparedTo.hashCode()) {
							populationSize = populationSize.add(BigDecimal.valueOf(count.get(dataset))
									.multiply(BigDecimal.valueOf(count.get(datasetComparedTo))));
						}
					}
				}
				populationSize = populationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap.get()), 0,
						RoundingMode.HALF_UP);
			}

			// calculate & store measurements
			for (Resource dataset : datasetsCoveringTheAspekt) {

				// calculate & store completeness:
				if (totalPairwiseOverlap.get() != 0) {
					/** Ratio of resources in an estimated population covered by this dataset */
					BigDecimal completeness = BigDecimal.valueOf(count.get(dataset)).divide(populationSize, 2,
							RoundingMode.HALF_UP);
					Collection<Resource> otherDatasets = new HashSet<>(datasetsCoveringTheAspekt);
					otherDatasets.remove(dataset);
					Metadata.addQualityMeasurement(AV.marCompletenessThomas08, completeness, OM.one, dataset,
							otherDatasets, aspect.getIri(), this.getOutputMetaModel(dataset));
				}

				// calculate relative coverage
				relativeCoverage.put(dataset, new HashMap<>());
				for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
					if (!dataset.equals(datasetComparedTo)) {
						int countComparedTo = count.get(datasetComparedTo);
						if (countComparedTo == 0) {
							relativeCoverage.get(dataset).put(datasetComparedTo, BigDecimal.ONE);
						} else {
							int overlap = absoluteCoverage.get(dataset).get(datasetComparedTo);
							relativeCoverage.get(dataset).put(datasetComparedTo, BigDecimal.valueOf(overlap)
									.divide(BigDecimal.valueOf(countComparedTo), 2, RoundingMode.HALF_UP));
						}
					}
				}
			}

			// measures
			for (Resource dataset : datasetsCoveringTheAspekt) {
				for (Resource datasetComparedTo : datasetsCoveringTheAspekt) {
					if (!dataset.equals(datasetComparedTo)) {
						Metadata.addQualityMeasurement(AV.relativeCoverage,
								relativeCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset,
								datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
						Metadata.addQualityMeasurement(AV.absoluteCoverage,
								absoluteCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset,
								datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
					}
				}
			}
		}
	}
}
