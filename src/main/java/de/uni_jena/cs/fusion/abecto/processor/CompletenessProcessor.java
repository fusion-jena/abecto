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
	/** Ratio of resources in an estimated polulation covered by this dataset. */
	Map<Resource, BigDecimal> completeness = new HashMap<>();
	/** Number of correspondence set found over all dataset. */
	int correspondenceSetsCount = 0;
	/** Number of overlaps between all pairs of dataset, excluding duplicates. */
	int totalPairwiseOverlap = 0;

	@Override
	public void run() {
		for (Resource aspectIri : aspects) {

			Aspect aspect = this.getAspects().get(aspectIri);

			// get resources by dataset and aspect
			Map<Resource, Set<Resource>> resourcesByDataset = new HashMap<>();
			for (Resource inputDataset : this.getInputDatasets()) {
				resourcesByDataset.put(inputDataset,
						Aspect.getResourceKeys(aspect, inputDataset, this.getInputPrimaryModelUnion(inputDataset)));
			}

			// prepare measurements
			for (Resource dataset : this.getInputDatasets()) {
				for (Resource datasetComparedTo : this.getInputDatasets()) {
					if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
						absoluteCoverage.computeIfAbsent(dataset, x -> new HashMap<>());
					}
				}
			}

			// process correspondence sets of aspect
			Correspondences.getCorrespondenceSets(this.getMetaModelUnion(null), aspectIri)
					.forEach(correspondingResources -> {
						// count number of correspondence sets
						correspondenceSetsCount++;

						Map<Resource, Set<Resource>> occurrencesByDataset = new HashMap<>();
						// count resources of the dataset in the correspondence set
						for (Resource dataset : this.getInputDatasets()) {
							Set<Resource> resourcesOfDataset = resourcesByDataset.get(dataset);
							for (Resource correspondingResource : correspondingResources) {
								if (resourcesOfDataset.contains(correspondingResource)) {
									occurrencesByDataset.computeIfAbsent(dataset, r -> new HashSet<>())
											.add(correspondingResource);
								}
							}
						}
						for (Resource dataset : this.getInputDatasets()) {
							int occurrences = occurrencesByDataset.get(dataset).size();
							if (occurrences == 0) {
								// count correspondence sets the dataset is not participating in
								omissions.merge(dataset, 1, Integer::sum);

								// report resource omission
								for (Resource datasetComparedTo : this.getInputDatasets()) {
									for (Resource resourceComparedTo : occurrencesByDataset.get(dataset)) {
										Metadata.addResourceOmission(dataset, datasetComparedTo, resourceComparedTo,
												aspect.getIri(), this.getOutputMetaModel(dataset));
									}
								}
							}
							if (occurrences > 0) {
								for (Resource datasetComparedTo : this.getInputDatasets()) {
									if (!occurrencesByDataset.get(datasetComparedTo).isEmpty()) {
										if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
											// only once per pair

											// count covered resources of the compared dataset
											absoluteCoverage.get(dataset).merge(datasetComparedTo, 1, Integer::sum);

											// count total pairwise overlap
											totalPairwiseOverlap++;
										}
									}
								}
								if (occurrences > 1) {
									// count duplicates, excluding the one to stay
									duplicates.merge(dataset, occurrences - 1, Integer::sum);
								}
							}
						}
					});

			// calculate resource count
			for (Resource dataset : this.getInputDatasets()) {
				count.put(dataset, resourcesByDataset.get(dataset).size() - duplicates.get(dataset));
			}

			// calculate population size
			BigDecimal populationSize = BigDecimal.ZERO;
			for (Resource dataset : this.getInputDatasets()) {
				for (Resource datasetComparedTo : this.getInputDatasets()) {
					if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
						populationSize = populationSize.add(BigDecimal.valueOf(count.get(dataset))
								.multiply(BigDecimal.valueOf(count.get(dataset))));
					}
				}
			}
			populationSize = populationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap), 0, RoundingMode.HALF_UP);

			// calculate measurements
			for (Resource dataset : this.getInputDatasets()) {

				// calculate completeness
				completeness.put(dataset,
						populationSize.divide(BigDecimal.valueOf(count.get(dataset)), 2, RoundingMode.HALF_UP));

				// calculate relative coverage
				relativeCoverage.put(dataset, new HashMap<>());
				for (Resource datasetComparedTo : this.getInputDatasets()) {
					if (!dataset.equals(datasetComparedTo)) {
						int countComparedTo = count.get(datasetComparedTo);
						int overlap = absoluteCoverage.get(dataset).get(datasetComparedTo);
						relativeCoverage.get(dataset).put(datasetComparedTo, BigDecimal.valueOf(overlap)
								.divide(BigDecimal.valueOf(countComparedTo), 2, RoundingMode.HALF_UP));
					}
				}
			}

			// store measures
			for (Resource dataset : this.getInputDatasets()) {
				// store count
				Metadata.addQualityMeasurement(AV.count, count.get(dataset), OM.one, dataset, aspect.getIri(),
						this.getOutputMetaModel(dataset));

				// store completeness
				Collection<Resource> otherDatasets = this.getInputDatasets();
				otherDatasets.remove(dataset);
				Metadata.addQualityMeasurement(AV.markAndRecapture, completeness.get(dataset), OM.one, dataset,
						otherDatasets, aspect.getIri(), this.getOutputMetaModel(dataset));

				for (Resource datasetComparedTo : this.getInputDatasets()) {
					if (!dataset.equals(datasetComparedTo)) {
						Metadata.addQualityMeasurement(AV.relativeCoverage,
								relativeCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset,
								datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
						if (dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
							// only once per pair

							Metadata.addQualityMeasurement(AV.absoluteCoverage,
									absoluteCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset,
									datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(dataset));
							Metadata.addQualityMeasurement(AV.absoluteCoverage,
									absoluteCoverage.get(datasetComparedTo).get(dataset), OM.one, dataset,
									datasetComparedTo, aspect.getIri(), this.getOutputMetaModel(datasetComparedTo));
						}
					}
				}
			}

		}
	}
}
