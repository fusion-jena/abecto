/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.processor;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uni_jena.cs.fusion.abecto.*;
import de.uni_jena.cs.fusion.abecto.measure.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

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
public class PopulationComparisonProcessor extends ComparisonProcessor<PopulationComparisonProcessor> {

    /**
     * The {@link Aspect Aspects} to process.
     */
    @Parameter
    public Collection<Resource> aspects;
    Aspect aspect;
    Set<Resource> datasets;
    Set<ResourcePair> datasetPairs;
    Set<ResourceTupel> datasetTupels;
    Map<Resource, Model> outputMetaModelByDataset;
    /**
     * Number of covered resources of another dataset, excluding duplicates.
     */
    AbsoluteCoverage absoluteCoverage = new AbsoluteCoverage();
    /**
     * Number of resources in this dataset including duplicates.
     */
    PerDatasetCount count = new PerDatasetCount(AV.count, OM.one);
    /**
     * Number of resource duplicates in this dataset.
     */
    PerDatasetCount duplicateCount = new PerDatasetCount(null, OM.one);// TODO define measure IRI
    /**
     * Number of resources in this dataset excluding duplicates.
     */
    PerDatasetCount deduplicatedCount = new PerDatasetCount(AV.deduplicatedCount, OM.one);
    RelativeCoverage relativeCoverage;
    Completeness completeness;

    Map<Resource, Set<Resource>> unprocessedResourcesByDataset = new HashMap<>();

    public void run() {
        for (Resource aspectIri : aspects) {
            Aspect aspect = this.getAspects().get(aspectIri);
            compareAspectPopulation(aspect);
        }
    }

    void compareAspectPopulation(Aspect aspect) {
        setAspect(aspect);
        setAspectDatasets();
        resetMeasures();
        loadResourcesOfAspect();

        measureResourceCounts();
        countAndReportCoverageAndDuplicatesAndOmissions(getCorrespondenceGroups());
        calculateDeduplicatedCount();
        relativeCoverage = RelativeCoverage.calculate(absoluteCoverage, deduplicatedCount);
        completeness = Completeness.calculate(absoluteCoverage, deduplicatedCount);

        count.storeInModel(aspect, outputMetaModelByDataset);
        deduplicatedCount.storeInModel(aspect, outputMetaModelByDataset);
        // TODO store duplicateCount (requires definition of measure IRI)
        absoluteCoverage.storeInModel(aspect, outputMetaModelByDataset);
        relativeCoverage.storeInModel(aspect, outputMetaModelByDataset);
        completeness.storeInModel(aspect, outputMetaModelByDataset);
        reportOmissionsOfUnprocessedResources();
    }

    private void setAspect(Aspect aspect) {
        this.aspect = aspect;
    }

    private void setAspectDatasets() {
        datasets = aspect.getDatasets();
        datasetPairs = ResourcePair.getPairsOf(datasets);
        datasetTupels = ResourceTupel.getTupelsOf(datasets);
        outputMetaModelByDataset = getOutputMetaModels(datasets);
    }

    private void loadResourcesOfAspect() {
        unprocessedResourcesByDataset.clear();
        for (Resource dataset : datasets) {
            loadResourcesOfAspectAndDataset(dataset);
        }
    }

    private void resetMeasures() {
        count.reset(datasets, 0L);
        deduplicatedCount.reset(datasets, 0L);
        duplicateCount.reset(datasets, 0L);
        absoluteCoverage.reset(datasetPairs, 0L);
    }

    private void countAndReportCoverageAndDuplicatesAndOmissions(Stream<List<Resource>> correspondenceGroups) {
        correspondenceGroups.forEach(this::countAndReportCoverageAndDuplicatesAndOmissions);
    }

    private void countAndReportCoverageAndDuplicatesAndOmissions(List<Resource> correspondingResources) {
        Map<Resource, Set<Resource>> correspondingResourcesByDataset = separateByDataset(correspondingResources);
        removeFromUnprocessedResources(correspondingResourcesByDataset);
        incrementAbsoluteCoverages(correspondingResourcesByDataset);
        incrementDuplicatesCount(correspondingResourcesByDataset);
        reportOmissions(correspondingResourcesByDataset);
        reportDuplicates(correspondingResourcesByDataset);
    }

    private void incrementAbsoluteCoverages(Map<Resource, Set<Resource>> correspondingResourcesByDataset) {
        for (ResourcePair datasetPair : datasetPairs) {
            if (!correspondingResourcesByDataset.get(datasetPair.first).isEmpty() &&
                    !correspondingResourcesByDataset.get(datasetPair.second).isEmpty()) {
                absoluteCoverage.incrementByOrSetOne(datasetPair);
            }
        }
    }

    private void reportOmissions(Map<Resource, Set<Resource>> correspondingResourcesByDataset) {
        for (Resource dataset : datasets) {
            if (correspondingResourcesByDataset.get(dataset).isEmpty()) {
                reportOmissionsForDataset(dataset, correspondingResourcesByDataset);
            }
        }
    }

    private void reportOmissionsForDataset(Resource dataset, Map<Resource, Set<Resource>> missingResourcesByDataset) {
        for (Resource datasetComparedTo : missingResourcesByDataset.keySet()) {
            for (Resource resourceComparedTo : missingResourcesByDataset.get(datasetComparedTo)) {
                Metadata.addResourceOmission(dataset, datasetComparedTo, resourceComparedTo,
                        aspect.getIri(), outputMetaModelByDataset.get(dataset));
            }
        }
    }

    private void incrementDuplicatesCount(Map<Resource, Set<Resource>> correspondingResourcesByDataset) {
        for (Resource dataset : datasets) {
            if (!correspondingResourcesByDataset.get(dataset).isEmpty()) {
                int occurrencesInDataset = correspondingResourcesByDataset.get(dataset).size();
                duplicateCount.incrementByOrSet(dataset, occurrencesInDataset - 1);
            }
        }
    }

    private void reportDuplicates(Map<Resource, Set<Resource>> correspondingResourcesByDataset) {
        for (Resource dataset : datasets) {
            if (!correspondingResourcesByDataset.get(dataset).isEmpty()) {
                for (Resource duplicateResource1 : correspondingResourcesByDataset.get(dataset)) {
                    for (Resource duplicateResource2 : correspondingResourcesByDataset.get(dataset)) {
                        if (!duplicateResource1.equals(duplicateResource2)) {
                            Metadata.addIssue(duplicateResource1, null, null, aspect.getIri(),
                                    "Duplicated Resource", "of <" + duplicateResource2 + ">",
                                    outputMetaModelByDataset.get(dataset)); // TODO use dedicated issue IRI
                        }
                    }
                }
            }
        }
    }

    private Map<Resource, Set<Resource>> separateByDataset(List<Resource> resources) {
        Map<Resource, Set<Resource>> resourcesByDataset = new HashMap<>();
        for (Resource dataset : datasets) {
            Set<Resource> resourcesOfDataset = new HashSet<>(resources);
            resourcesOfDataset.retainAll(unprocessedResourcesByDataset.get(dataset));
            resourcesByDataset.put(dataset, resourcesOfDataset);
        }
        return resourcesByDataset;
    }

    private void removeFromUnprocessedResources(Map<Resource, Set<Resource>> coveredResourcesByDataset) {
        for (Resource dataset : coveredResourcesByDataset.keySet()) {
            Set<Resource> unprocessedResourcesOfDataset = unprocessedResourcesByDataset.get(dataset);
            Set<Resource> coveredResourcesOfDataset = coveredResourcesByDataset.get(dataset);
            unprocessedResourcesOfDataset.removeAll(coveredResourcesOfDataset);
        }
    }

    private void calculateDeduplicatedCount() {
        deduplicatedCount.setDifferenceOf(count, duplicateCount);
    }

    private void reportOmissionsOfUnprocessedResources() {
        for (ResourcePair datasetPair : datasetPairs) {
            reportOmissionsOfUnprocessedResourcesForResource(datasetPair.first, datasetPair.second);
            reportOmissionsOfUnprocessedResourcesForResource(datasetPair.second, datasetPair.first);
        }
    }

    private void reportOmissionsOfUnprocessedResourcesForResource(Resource dataset, Resource datasetComparedTo) {
        for (Resource unprocessedResource : unprocessedResourcesByDataset.get(datasetComparedTo)) {
            Metadata.addResourceOmission(dataset, datasetComparedTo, unprocessedResource, aspect.getIri(),
                    outputMetaModelByDataset.get(dataset));
        }
    }

    private void loadResourcesOfAspectAndDataset(Resource dataset) {
        Set<Resource> distinctResources = getResourceKeys(aspect, dataset).collect(Collectors.toSet());
        unprocessedResourcesByDataset.put(dataset, distinctResources);
    }

    private void measureResourceCounts() {
        for (Resource dataset : datasets) {
            count.set(dataset, (long) unprocessedResourcesByDataset.get(dataset).size());
        }
    }
}
