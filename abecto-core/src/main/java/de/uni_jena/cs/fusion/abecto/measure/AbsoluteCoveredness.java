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
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AbsoluteCoveredness extends PerDatasetLongMeasure {

    public AbsoluteCoveredness() {
        super(null, OM.one); // TODO define measure IRI
    }

    public static Map<String, AbsoluteCoveredness> createMapByVariable(Iterable<String> variables) {
        Map<String, AbsoluteCoveredness> mapOfCounts = new HashMap<>();
        for (String variable : variables) {
            AbsoluteCoveredness countOfVariable = new AbsoluteCoveredness();
            countOfVariable.setVariable(variable);
            mapOfCounts.put(variable, countOfVariable);
        }
        return mapOfCounts;
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
