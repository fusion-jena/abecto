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

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.processor.PopulationComparisonProcessor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.openjdk.jmh.annotations.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class PopulationComparisonProcessorBenchmark {

    @Benchmark
    @Fork(value = 1, warmups = 2)
    @Warmup(iterations = 5)
    @Measurement(iterations = 10)
    @BenchmarkMode(Mode.SingleShotTime)
    public Model benchmark(Configuration configuration) {
        PopulationComparisonProcessor processor = configuration.processor;
        processor.run();
        return processor.getOutputMetaModel(ResourceFactory.createResource("0"));
    }

    @State(Scope.Benchmark)
    public static class Configuration {
        @Param({"100", "1000", "10000", "100000"})
        public int populationSize;
        @Param({"2", "3", "4", "5", "6", "7", "8", "9", "10"})
        public int datasetCount;
        @Param({"0.25", "0.5", "0.75"})
        public double coverage;
        public PopulationComparisonProcessor processor;

        @Setup(Level.Iteration)
        public void setup() {
            processor = new PopulationComparisonProcessorBenchmark.IndependentPopulationComparisonProcessor(this.populationSize, this.datasetCount, this.coverage);
        }
    }

    private static class IndependentPopulationComparisonProcessor extends PopulationComparisonProcessor {

        private final ComparisonBenchmarkDataSupplier dataSupplier;

        public IndependentPopulationComparisonProcessor(int populationSize, int datasetCount, double coverage) {
            this.dataSupplier = new ComparisonBenchmarkDataSupplier(populationSize, datasetCount, coverage, 0);
            Resource aspectIri = ResourceFactory.createResource("aspect");
            this.aspects = Collections.singleton(aspectIri);
            Aspect aspect = new Aspect(aspectIri, "key");
            Query query = QueryFactory.create("SELECT ?var1 WHERE {?key ?p ?var1}");
            for (Resource dataset : dataSupplier.getDatasets()) {
                aspect.setPattern(dataset, query);
            }
            this.addAspects(aspect);
        }

        @Override
        public Set<Resource> getDatasets() {
            return this.dataSupplier.getDatasets();
        }

        @Override
        public Stream<Resource> getResourceKeys(Aspect aspect, Resource dataset) throws NullPointerException {
            return this.dataSupplier.getResourceKeys(dataset);
        }

        public Stream<List<Resource>> getCorrespondenceGroups() {
            return this.dataSupplier.getCorrespondenceGroups();
        }
    }
}
