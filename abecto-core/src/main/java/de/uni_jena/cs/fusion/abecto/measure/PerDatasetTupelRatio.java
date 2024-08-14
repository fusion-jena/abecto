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
import java.util.Map;

public class PerDatasetTupelRatio extends Ratio<ResourceTupel> {

    public PerDatasetTupelRatio(Resource quantity, Resource unit) {
        super(quantity, unit);
    }

    public void setRatioOf(PerDatasetPairCount numerators, PerDatasetCount denominators) {
        for (ResourcePair pair : numerators.keySet()) {
            BigDecimal numerator = BigDecimal.valueOf(numerators.get(pair));
            if (denominators.contains(pair.first)) {
                BigDecimal denominator = BigDecimal.valueOf(denominators.get(pair.first));
                BigDecimal value = numerator.divide(denominator, SCALE, ROUNDING_MODE);
                set(ResourceTupel.getTupel(pair.first, pair.second), value);
            }
            if (denominators.contains(pair.second)) {
                BigDecimal denominator = BigDecimal.valueOf(denominators.get(pair.second));
                BigDecimal value = numerator.divide(denominator, SCALE, ROUNDING_MODE);
                set(ResourceTupel.getTupel(pair.second, pair.first), value);
            }
        }
    }

    public void storeInModel(Aspect aspect, Map<Resource, Model> outputModelsMap) {
        for (ResourceTupel tupel : keySet()) {
            Metadata.addQualityMeasurement(quantity, get(tupel), unit,
                    tupel.first, tupel.second, aspect.getIri(), outputModelsMap.get(tupel.first));
        }
    }

    public void storeInModelWithVariable(Aspect aspect, String variable, Map<Resource, Model> outputModelsMap) {
        for (ResourceTupel tupel : keySet()) {
            Metadata.addQualityMeasurement(quantity, get(tupel), unit,
                    tupel.first, variable, tupel.second, aspect.getIri(), outputModelsMap.get(tupel.first));
        }
    }
}
