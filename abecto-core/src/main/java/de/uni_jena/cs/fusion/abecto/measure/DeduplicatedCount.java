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

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;

import java.util.HashMap;
import java.util.Map;

public class DeduplicatedCount extends PerDatasetLongMeasure {

    public DeduplicatedCount() {
        super(AV.deduplicatedCount, OM.one);
    }

    public static DeduplicatedCount calculate(Count count, DuplicateCount duplicateCount) {
        DeduplicatedCount deduplicatedCount = new DeduplicatedCount();
        deduplicatedCount.setDifferenceOf(count, duplicateCount);
        return deduplicatedCount;
    }

    public static Map<String, DeduplicatedCount> createMapByVariable(Iterable<String> variables) {
        Map<String, DeduplicatedCount> mapOfCounts = new HashMap<>();
        for (String variable : variables) {
            DeduplicatedCount countOfVariable = new DeduplicatedCount();
            countOfVariable.setVariable(variable);
            mapOfCounts.put(variable, countOfVariable);
        }
        return mapOfCounts;
    }

}
