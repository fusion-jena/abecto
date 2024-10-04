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
import de.uni_jena.cs.fusion.abecto.ResourcePair;
import de.uni_jena.cs.fusion.abecto.ResourceTupel;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class RelativeCoverage extends BigDecimalMeasure<ResourceTupel> {

    public RelativeCoverage() {
        super(AV.relativeCoverage, OM.one);
    }

    public static RelativeCoverage calculate(AbsoluteCoverage absoluteCoverage, DeduplicatedCount deduplicatedCount) {
        RelativeCoverage relativeCoverage = new RelativeCoverage();
        Set<ResourcePair> datasetPairs = getDatasetPairsWithSufficientData(absoluteCoverage, deduplicatedCount);
        for (ResourcePair datasetPair : datasetPairs) {
            BigDecimal absoluteCoverageOfPair = BigDecimal.valueOf(absoluteCoverage.get(datasetPair));
            relativeCoverage.setRatioForTupel(absoluteCoverageOfPair, deduplicatedCount, datasetPair.first, datasetPair.second);
            relativeCoverage.setRatioForTupel(absoluteCoverageOfPair, deduplicatedCount, datasetPair.second, datasetPair.first);
        }
        return relativeCoverage;
    }

    private static Set<ResourcePair> getDatasetPairsWithSufficientData(AbsoluteCoverage absoluteCoverage, DeduplicatedCount deduplicatedCount) {
        Set<ResourcePair> datasetPairsWithAbsoluteCoverage = absoluteCoverage.keySet();
        Set<Resource> datasetsWithDeduplicatedCount = deduplicatedCount.keySet();
        return ResourcePair.getPairsBothContainedIn(datasetPairsWithAbsoluteCoverage, datasetsWithDeduplicatedCount);
    }

    void setRatioForTupel(BigDecimal numerator, DeduplicatedCount denominators, Resource assessedDataset, Resource comparedDataset) {
        BigDecimal denominator = BigDecimal.valueOf(denominators.get(comparedDataset));
        if (!denominator.equals(BigDecimal.ZERO)) {
            BigDecimal value = numerator.divide(denominator, SCALE, ROUNDING_MODE);
            set(ResourceTupel.getTupel(assessedDataset, comparedDataset), value);
        }
    }

    public void storeInModel(Aspect aspect, Map<Resource, Model> outputModelsMap) {
        for (ResourceTupel tupel : keySet()) {
            storeInModel(aspect, tupel.first, tupel.second, get(tupel), outputModelsMap.get(tupel.first));
        }
    }
}
