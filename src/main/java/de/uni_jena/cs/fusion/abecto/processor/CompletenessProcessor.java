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

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

/**
 * Provides measurements for <strong>number of duplicates</strong>,
 * <strong>absolute coverage</strong>, <strong>relative coverage</strong>, and
 * <strong>completeness</strong> per aspect, as well as <strong>resource
 * omission</strong> and <strong>duplicate</strong> annotations.
 */
public class CompletenessProcessor extends Processor<CompletenessProcessor> {

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
			/** Ratio of resources in an estimated population covered by this dataset. */
			Map<Resource, BigDecimal> completeness = new HashMap<>();
			/** Number of overlaps between all pairs of dataset, excluding duplicates. */
			AtomicInteger totalPairwiseOverlap = new AtomicInteger(0);

			Aspect aspect = this.getAspects().get(aspectIri);

			// get resources by dataset and aspect
			Map<Resource, Set<Resource>> resourcesByDataset = new HashMap<>();
			for (Resource dataset : this.getDatasets()) {
				resourcesByDataset.put(dataset,
						Aspect.getResourceKeys(aspect, dataset, this.getInputPrimaryModelUnion(dataset)));
			}

			// prepare measurements
			for (Resource dataset : this.getDatasets()) {
				for (Resource datasetComparedTo : this.getDatasets()) {
					absoluteCoverage.computeIfAbsent(dataset, x -> new HashMap<>()).put(datasetComparedTo, 0);
				}
			}

			// process correspondence sets of aspect
			Correspondences.getCorrespondenceSets(this.getMetaModelUnion(null), aspectIri)
					.forEach(correspondingResources -> {
						Map<Resource, Set<Resource>> occurrencesByDataset = new HashMap<>();
						// count resources of the dataset in the correspondence set
						for (Resource dataset : this.getDatasets()) {
							Set<Resource> resourcesOfDataset = resourcesByDataset.get(dataset);
							Set<Resource> occurrencesOfDataset = occurrencesByDataset.computeIfAbsent(dataset,
									r -> new HashSet<>());
							for (Resource correspondingResource : correspondingResources) {
								if (resourcesOfDataset.contains(correspondingResource)) {
									occurrencesOfDataset.add(correspondingResource);
								}
							}
						}
						for (Resource dataset : this.getDatasets()) {
							int occurrences = occurrencesByDataset.containsKey(dataset)
									? occurrencesByDataset.get(dataset).size()
									: 0;
							if (occurrences == 0) {
								// count correspondence sets the dataset is not participating in
								omissions.merge(dataset, 1, Integer::sum);

								// report resource omission
								for (Resource datasetComparedTo : this.getDatasets()) {
									for (Resource resourceComparedTo : occurrencesByDataset.get(dataset)) {
										Metadata.addResourceOmission(dataset, datasetComparedTo, resourceComparedTo,
												aspect.getIri(), this.getOutputMetaModel(dataset));
									}
								}
							}
							if (occurrences > 0) {
								for (Resource datasetComparedTo : this.getDatasets()) {
									if (!occurrencesByDataset.get(datasetComparedTo).isEmpty()) {
										if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
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
					});

			// calculate resource count
			for (Resource dataset : this.getDatasets()) {
				count.put(dataset, resourcesByDataset.get(dataset).size() - duplicates.getOrDefault(dataset, 0));
			}

			// calculate population size
			BigDecimal populationSize = BigDecimal.ZERO;
			for (Resource dataset : this.getDatasets()) {
				for (Resource datasetComparedTo : this.getDatasets()) {
					if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
						populationSize = populationSize.add(BigDecimal.valueOf(count.get(dataset))
								.multiply(BigDecimal.valueOf(count.get(dataset))));
					}
				}
			}
			populationSize = populationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap.get()), 0,
					RoundingMode.HALF_UP);

			// calculate measurements
			for (Resource dataset : this.getDatasets()) {

				// calculate completeness
				completeness.put(dataset,
						populationSize.divide(BigDecimal.valueOf(count.get(dataset)), 2, RoundingMode.HALF_UP));

				// calculate relative coverage
				relativeCoverage.put(dataset, new HashMap<>());
				for (Resource datasetComparedTo : this.getDatasets()) {
					if (!dataset.equals(datasetComparedTo)) {
						int countComparedTo = count.get(datasetComparedTo);
						int overlap = absoluteCoverage.get(dataset).get(datasetComparedTo);
						relativeCoverage.get(dataset).put(datasetComparedTo, BigDecimal.valueOf(overlap)
								.divide(BigDecimal.valueOf(countComparedTo), 2, RoundingMode.HALF_UP));
					}
				}
			}

			// store measures
			for (Resource dataset : this.getDatasets()) {
				// store count
				Metadata.addQualityMeasurement(AV.count, count.get(dataset), OM.one, dataset, aspect.getIri(),
						this.getOutputMetaModel(dataset));

				// store completeness
				Collection<Resource> otherDatasets = this.getDatasets();
				otherDatasets.remove(dataset);
				Metadata.addQualityMeasurement(AV.markAndRecapture, completeness.get(dataset), OM.one, dataset,
						otherDatasets, aspect.getIri(), this.getOutputMetaModel(dataset));

				for (Resource datasetComparedTo : this.getDatasets()) {
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

			// TODO store discovered resource omissions

		}
	}
}
