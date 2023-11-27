/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.benchmark;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import static de.uni_jena.cs.fusion.abecto.Metadata.getQualityMeasurement;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PopulationComparisonProcessorBenchmarkTest {
    @Test
    public void benchmark() {
        PopulationComparisonProcessorBenchmark.Configuration configuration = new PopulationComparisonProcessorBenchmark.Configuration();
        configuration.populationSize = 10000;
        configuration.datasetCount = 2;
        configuration.coverage = 0.75;
        configuration.setup();

        Model outputModel = new PopulationComparisonProcessorBenchmark().benchmark(configuration);

        Resource aspect = ResourceFactory.createResource("aspect");
        Resource dataset0 = ResourceFactory.createResource("0");
        Resource dataset1 = ResourceFactory.createResource("1");
        Collection<Resource> dataset1singleton = Collections.singleton(dataset1);
        Collection<Resource> none = Collections.emptySet();

        assertEquals(10000, getQualityMeasurement(AV.count, dataset0, null, none, aspect, outputModel).value);
        assertEquals(10000, getQualityMeasurement(AV.deduplicatedCount, dataset0, null, none, aspect, outputModel).value);
        assertEquals(7500, getQualityMeasurement(AV.absoluteCoverage, dataset0, null, dataset1singleton, aspect, outputModel).value);
        assertEquals(0.75, getQualityMeasurement(AV.relativeCoverage, dataset0, null, dataset1singleton, aspect, outputModel).value.doubleValue());
        assertEquals(0.7500187504687617, getQualityMeasurement(AV.marCompletenessThomas08, dataset0, null, dataset1singleton, aspect, outputModel).value.doubleValue());
    }
}
