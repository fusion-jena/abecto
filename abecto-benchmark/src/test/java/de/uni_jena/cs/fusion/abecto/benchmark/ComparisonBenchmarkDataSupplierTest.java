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

package de.uni_jena.cs.fusion.abecto.benchmark;

import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ComparisonBenchmarkDataSupplierTest {

    @ParameterizedTest(name = "[{index}] Data generation for {0} datasources.")
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
    public void supplierTest(int datasetCount) {
        int populationSize = 1000;
        double coverage = 0.75, errorRate = 0.1;
        ComparisonBenchmarkDataSupplier supplier = new ComparisonBenchmarkDataSupplier(populationSize, datasetCount,
                coverage, errorRate);

        // check dataset count
        List<Resource> dataset = new ArrayList<>(supplier.getDatasets());
        Assertions.assertEquals(datasetCount, dataset.size());

        // check population size
        Set<Resource>[] population = new Set[datasetCount];
        for (int i = 0; i < datasetCount; i++) {
            population[i] = supplier.getResourceKeys(dataset.get(i)).collect(Collectors.toSet());
            Assertions.assertEquals(populationSize, population[i].size());
        }

        // check values
        for (int i = 0; i < datasetCount; i++) {
            int rightValuesSingle = 0, wrongValuesSingle = 0;
            for (Resource resource : population[i]) {
                Assertions.assertEquals(1, supplier.getValuesByVariable(resource, dataset.get(i),
                        Collections.singletonList("var")).get("var").size());
                if (supplier.getValuesByVariable(resource, dataset.get(i), Collections.singletonList("var")).get(
                        "var").contains(ComparisonBenchmarkDataSupplier.correctValue)) {
                    rightValuesSingle++;
                } else {
                    wrongValuesSingle++;
                }
            }
            Assertions.assertEquals((int) (populationSize * errorRate), wrongValuesSingle);
            Assertions.assertEquals(populationSize - (int) (populationSize * errorRate), rightValuesSingle);
        }


        // pairwise check coverage
        int[][] pairs = new int[datasetCount-1][datasetCount];
        supplier.getCorrespondenceGroups().forEach(resources -> {
            for (int i = 0; i < datasetCount; i++) {
                if (resources.stream().anyMatch(population[i]::contains)) {
                    for (int j = i + 1; j < datasetCount; j++) {
                        if (resources.stream().anyMatch(population[j]::contains)) {
                            pairs[i][j]++;
                        }
                    }
                }
            }
        });
        int expectedCoverageMax = (int) (populationSize * coverage);
        // allow underrun by 1 per overlap case the pair is involved (2^{n-2}) and per right / wrong value case (x2)
        int expectedCoverageMin = (int) (expectedCoverageMax - (Math.pow(2, datasetCount - 2) * 2));
        int exampleCoverage = pairs[0][1];
        for (int i = 0; i < datasetCount; i++) {
            for (int j = i + 1; j < datasetCount; j++) {
                // check for equal coverage of all pairs
                Assertions.assertEquals(exampleCoverage,pairs[i][j]);
                //check for a range to accept underrun due to rounding
                Assertions.assertTrue(expectedCoverageMin <= pairs[i][j] && pairs[i][j] <= expectedCoverageMax,
                        String.format("%s <= %s <= %s", expectedCoverageMin, pairs[i][j], expectedCoverageMax));
            }
        }
    }
}
