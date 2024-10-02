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

package de.uni_jena.cs.fusion.abecto.measure;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.ResourcePair;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Completeness extends Ratio<Resource> {

    public Completeness() {
        super(AV.marCompletenessThomas08, OM.one);
    }

    public static Completeness calculate(AbsoluteCoverage absoluteCoverage, PerDatasetCount deduplicatedCount) {
        Set<ResourcePair> datasetPairs = getDatasetPairsWithSufficientData(absoluteCoverage, deduplicatedCount);
        long totalPairwiseOverlap = calculateTotalPairwiseOverlap(datasetPairs, absoluteCoverage);
        if (totalPairwiseOverlap != 0) {
            BigDecimal estimatedPopulationSize = calculateEstimatedPopulationSize(datasetPairs, deduplicatedCount, totalPairwiseOverlap);
            Set<Resource> datasets = ResourcePair.getResourcesOfPairs(datasetPairs);
            return calculateCompleteness(datasets, deduplicatedCount, estimatedPopulationSize);
        }
        return new Completeness(); // empty
    }

    private static Set<ResourcePair> getDatasetPairsWithSufficientData(AbsoluteCoverage absoluteCoverage, PerDatasetCount deduplicatedCount) {
        Set<ResourcePair> datasetPairs = absoluteCoverage.keySet();
        Set<Resource> datasetsWithDeduplicatedCount = deduplicatedCount.keySet();
        return ResourcePair.getPairsBothContainedIn(datasetPairs, datasetsWithDeduplicatedCount);
    }

    private static long calculateTotalPairwiseOverlap(Iterable<ResourcePair> datasetPairs, AbsoluteCoverage absoluteCoverage) {
        long totalPairwiseOverlap = 0L;
        for (ResourcePair datasetPair : datasetPairs) {
            if (absoluteCoverage.contains(datasetPair)) {
                totalPairwiseOverlap += absoluteCoverage.get(datasetPair);
            }
        }
        return totalPairwiseOverlap;
    }

    private static BigDecimal calculateEstimatedPopulationSize(Iterable<ResourcePair> datasetPairs, PerDatasetCount deduplicatedCount, long totalPairwiseOverlap) {
        BigDecimal estimatedPopulationSize = BigDecimal.ZERO;
        for (ResourcePair datasetPair : datasetPairs) {
            BigDecimal deduplicatedCount1 = BigDecimal.valueOf(deduplicatedCount.get(datasetPair.first));
            BigDecimal deduplicatedCount2 = BigDecimal.valueOf(deduplicatedCount.get(datasetPair.second));
            estimatedPopulationSize = estimatedPopulationSize.add(deduplicatedCount1.multiply(deduplicatedCount2));
        }
        estimatedPopulationSize = estimatedPopulationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap), SCALE,
                RoundingMode.HALF_UP);
        return estimatedPopulationSize;
    }

    private static Completeness calculateCompleteness(Iterable<Resource> datasets, PerDatasetCount deduplicatedCount, BigDecimal estimatedPopulationSize) {
        Completeness completeness = new Completeness();
        for (Resource dataset : datasets) {
            BigDecimal numerator = BigDecimal.valueOf(deduplicatedCount.get(dataset));
            BigDecimal completenessOfDataset = numerator.divide(estimatedPopulationSize, SCALE, ROUNDING_MODE);
            completeness.set(dataset, completenessOfDataset);
        }
        return completeness;
    }

    public void storeInModel(Aspect aspect, Map<Resource, Model> outputModelsMap) {
        for (Resource dataset : values.keySet()) {
            Collection<Resource> otherDatasets = new HashSet<>(values.keySet());
            otherDatasets.remove(dataset);
            Metadata.addQualityMeasurement(quantity, get(dataset), unit, dataset, variable,
                    otherDatasets, aspect.getIri(), outputModelsMap.get(dataset));
        }
    }
}
