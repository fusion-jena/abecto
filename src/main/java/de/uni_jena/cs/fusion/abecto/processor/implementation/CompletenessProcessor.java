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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Aspects;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.util.Correspondences;
import de.uni_jena.cs.fusion.abecto.util.Metadata;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

/**
 * Provides measurements for <strong>number of duplicates</strong>,
 * <strong>absolute coverage</strong>, and <strong>relative coverage</strong>
 * per aspect, as well as <strong>resource omission</strong> and
 * <strong>duplicate</strong> annotations.
 * <p>
 * <strong>Note:</strong> Absolute coverage might not be symmetric in case of
 * duplicates.
 * <p>
 * TODO measure <strong>completeness</strong>
 */
public class CompletenessProcessor extends Processor {

	/**
	 * The {@link Aspect Aspects} to process.
	 */
	@Parameter
	public Collection<Resource> aspects;

	/** Number of resources in another dataset that are covered by this dataset. */
	Map<Resource, Map<Resource, Integer>> absoluteCoverage = new HashMap<>();
	/** Number of duplicate resources in this dataset, excluding the on to stay. */
	Map<Resource, Integer> duplicates = new HashMap<>();
	/** Number of resources in this dataset, excluding duplicates. */
	Map<Resource, Integer> count = new HashMap<>();
	/** Number of resources in this dataset including duplicates. */
	Map<Resource, Integer> total = new HashMap<>();
	/** Number of resources not covered by this dataset. */
	Map<Resource, Integer> omissions = new HashMap<>();
	/** Number of resources not covered by any other dataset. */
	Map<Resource, Integer> unique = new HashMap<>();
	/** Ratio of resources in an estimated polulation covered by this dataset. */
	Map<Resource, Integer> completeness = new HashMap<>();
	/** Ratio of resources in another dataset covered by this dataset. */
	Map<Resource, Map<Resource, Integer>> relativeCoverage = new HashMap<>();
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
						Aspects.getResourceKeys(aspect, inputDataset, this.getInputPrimaryModelUnion(inputDataset)));
			}

			// prepare measurements
			for (Resource dataset1 : this.getInputDatasets()) {
				// get total number of resources per dataset
				total.put(dataset1, resourcesByDataset.get(dataset1).size());
				for (Resource dataset2 : this.getInputDatasets()) {
					if (!dataset1.equals(dataset2)) {
						// NOTE: In case of duplicates absolute coverage is not symmetric
						absoluteCoverage.computeIfAbsent(dataset1, x -> new HashMap<>());
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
												aspect, this.getOutputMetaModel(dataset));
									}
								}
							}
							if (occurrences > 0) {
								for (Resource datasetComparedTo : this.getInputDatasets()) {
									if (!dataset.equals(datasetComparedTo)) {
										int coverage = occurrencesByDataset.get(datasetComparedTo).size();
										// count covered resources of the compared dataset
										absoluteCoverage.get(dataset).merge(datasetComparedTo, coverage, Integer::sum);
										if (coverage > 0
												&& dataset.getURI().compareTo(datasetComparedTo.getURI()) > 0) {
											// for compared datasets also in the correspndence set, only once per pair

											// count total pairwise overlap
											totalPairwiseOverlap++;
										}
									}
								}
							}
							if (occurrences > 1) {
								// count duplicates, excluding the one to stay
								duplicates.merge(dataset, occurrences - 1, Integer::sum);
							}
						}
					});

			for (Resource dataset : this.getInputDatasets()) {
				// calculate unique resource
				unique.put(dataset, total.get(dataset) - correspondenceSetsCount + omissions.get(dataset)
						- duplicates.get(dataset));
				// calculate resource count
				count.put(dataset, total.get(dataset) - duplicates.get(dataset));
			}

			// calculate measurements
			for (Resource dataset : this.getInputDatasets()) {

				// TODO calculate completeness

				relativeCoverage.put(dataset, new HashMap<>());
				for (Resource datasetComparedTo : this.getInputDatasets()) {
					if (!dataset.equals(datasetComparedTo)) {
						// calculate relative coverage
						int totalComparedTo = total.get(datasetComparedTo);
						int overlap = absoluteCoverage.get(dataset).get(datasetComparedTo);
						relativeCoverage.get(dataset).put(datasetComparedTo, overlap / totalComparedTo);
					}
				}
			}

			// store measures
			for (Resource dataset : this.getInputDatasets()) {
				Collection<Resource> otherDatasets = this.getInputDatasets();
				otherDatasets.remove(dataset);

				// TODO store completeness
				// Metadata.addQualityMeasurement(AV., OM.one, dataset, otherDatasets, aspect,
				// this.getOutputMetaModel(dataset));
				Metadata.addQualityMeasurement(AV.count, count.get(dataset), OM.one, dataset, aspect,
						this.getOutputMetaModel(dataset));

				for (Resource datasetComparedTo : this.getInputDatasets()) {
					Metadata.addQualityMeasurement(AV.absoluteCoverage,
							relativeCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset, datasetComparedTo,
							aspect, this.getOutputMetaModel(dataset));
					Metadata.addQualityMeasurement(AV.relativeCoverage,
							relativeCoverage.get(dataset).get(datasetComparedTo), OM.one, dataset, datasetComparedTo,
							aspect, this.getOutputMetaModel(dataset));
				}
			}

		}
	}
}
