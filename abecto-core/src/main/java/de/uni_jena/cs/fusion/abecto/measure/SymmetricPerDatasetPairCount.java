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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class SymmetricPerDatasetPairCount extends Count<ResourcePair> {

    public SymmetricPerDatasetPairCount(Resource quantity, Resource unit) {
        super(quantity, unit);
    }

    public static Map<String, SymmetricPerDatasetPairCount> createMapByVariable(Iterable<String> variables, Resource quantity, Resource unit) {
        Map<String, SymmetricPerDatasetPairCount> mapBaVariable = new HashMap<>();
        for (String variable : variables) {
            SymmetricPerDatasetPairCount countOfVariable = new SymmetricPerDatasetPairCount(quantity, unit);
            countOfVariable.setVariable(variable);
            mapBaVariable.put(variable, countOfVariable);
        }
        return mapBaVariable;
    }

    public void storeInModel(Aspect aspect, Map<Resource, Model> outputModelsMap) {
        for (ResourcePair pair : keySet()) {
            Metadata.addQualityMeasurement(quantity, get(pair), unit, pair.first, variable, pair.second, aspect.getIri(), outputModelsMap.get(pair.first));
            Metadata.addQualityMeasurement(quantity, get(pair), unit, pair.second, variable, pair.first, aspect.getIri(), outputModelsMap.get(pair.second));
        }
    }
}
