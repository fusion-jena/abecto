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

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.processor.PropertyComparisonProcessor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.stream.Stream;

public class PropertyComparisonProcessorBenchmark {

    @Benchmark
    @Fork(value = 1, warmups = 2)
    @Warmup(iterations = 5)
    @Measurement(iterations = 10)
    @BenchmarkMode(Mode.SingleShotTime)
    public Model benchmark(Configuration configuration) {
        PropertyComparisonProcessor processor = configuration.processor;
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
        @Param({"0.1"})
        public double errorRate;
        public PropertyComparisonProcessor processor;

        @Setup(Level.Iteration)
        public void setup() {
            processor = new IndependentPropertyComparisonProcessor(this.populationSize, this.datasetCount, this.coverage, this.errorRate);
        }
    }

    private static class IndependentPropertyComparisonProcessor extends PropertyComparisonProcessor {

        private final ComparisonBenchmarkDataSupplier dataSupplier;
        private final String variable;

        public IndependentPropertyComparisonProcessor(int populationSize, int datasetCount, double coverage, double errorRate) {
            dataSupplier = new ComparisonBenchmarkDataSupplier(populationSize, datasetCount, coverage, errorRate);
            aspect = ResourceFactory.createResource("aspect");
            variable = "var1";
            variables = Collections.singletonList(variable);
            Aspect aspect = new Aspect(this.aspect, "key");
            Query query = QueryFactory.create("SELECT ?var1 WHERE {?key ?p ?var1}");
            for (Resource dataset : dataSupplier.getDatasets()) {
                aspect.setPattern(dataset, query);
            }
            addAspects(aspect);
        }

        @Override
        public Set<Resource> getDatasets() {
            return this.dataSupplier.getDatasets();
        }

        @Override
        public Stream<Resource> getResourceKeys(@SuppressWarnings("unused") Aspect aspect, Resource dataset) {
            return this.dataSupplier.getResourceKeys(dataset);
        }

        @Override
        protected Map<String, Set<RDFNode>> getValuesByVariable(Resource dataset, Resource resource) {
            Set<RDFNode> valuesOfVariable = dataSupplier.getValueOfResource(resource, dataset);
            return Collections.singletonMap(variable, valuesOfVariable);
        }

        @Override
        public Stream<List<Resource>> getCorrespondenceGroups() {
            return this.dataSupplier.getCorrespondenceGroups();
        }
    }
}
