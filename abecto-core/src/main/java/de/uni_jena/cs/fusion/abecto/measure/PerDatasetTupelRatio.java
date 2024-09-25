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
import de.uni_jena.cs.fusion.abecto.ResourceTupel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PerDatasetTupelRatio extends Ratio<ResourceTupel> {

    public PerDatasetTupelRatio(Resource quantity, Resource unit) {
        super(quantity, unit);
    }

    public static Map<String, PerDatasetTupelRatio> createMapByVariable(Iterable<String> variables, Resource quantity, Resource unit) {
        Map<String, PerDatasetTupelRatio> mapByVariable = new HashMap<>();
        for (String variable : variables) {
            PerDatasetTupelRatio ratioOfVariable = new PerDatasetTupelRatio(quantity, unit);
            ratioOfVariable.setVariable(variable);
            mapByVariable.put(variable, ratioOfVariable);
        }
        return mapByVariable;
    }

    public void setRatioOf(SymmetricPerDatasetPairCount numerators, PerDatasetCount denominators) {
        for (ResourcePair datasetPair : numerators.keySet()) {
            if (denominators.contains(datasetPair.first) && denominators.contains(datasetPair.second)) {
                BigDecimal numerator = BigDecimal.valueOf(numerators.get(datasetPair));
                setRatioForTupel(numerator, denominators, datasetPair.first, datasetPair.second);
                setRatioForTupel(numerator, denominators, datasetPair.second, datasetPair.first);
            }
        }
    }

    void setRatioForTupel(BigDecimal numerator, PerDatasetCount denominators, Resource assessedDataset, Resource comparedDataset) {
        BigDecimal denominator = BigDecimal.valueOf(denominators.get(comparedDataset));
        if (!denominator.equals(BigDecimal.ZERO)) {
            BigDecimal value = numerator.divide(denominator, SCALE, ROUNDING_MODE);
            set(ResourceTupel.getTupel(assessedDataset, comparedDataset), value);
        }
    }

    public void storeInModelWithVariable(Aspect aspect, String variable, Map<Resource, Model> outputModelsMap) {
        for (ResourceTupel tupel : keySet()) {
            Metadata.addQualityMeasurement(quantity, get(tupel), unit,
                    tupel.first, variable, tupel.second, aspect.getIri(), outputModelsMap.get(tupel.first));
        }
    }
}
