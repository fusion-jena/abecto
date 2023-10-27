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
package de.uni_jena.cs.fusion.abecto.processor;

import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.*;
import java.util.stream.Stream;

public class PropertyComparisonProcessorBenchmark {

    private static class IndependentPropertyComparisonProcessor extends PropertyComparisonProcessor {

        private final ComparisonBenchmarkDataSupplier dataSupplier;

        public IndependentPropertyComparisonProcessor(int populationSize, int datasetCount, double coverage) {
            this.dataSupplier = new ComparisonBenchmarkDataSupplier(populationSize, datasetCount, coverage, 0);
        }

        @Override
        public Set<Resource> getDatasets() {
            return this.dataSupplier.getDatasets();
        }

        @Override
        public Stream<Resource> getResourceKeys(@SuppressWarnings("unused") Aspect aspect, Resource dataset) throws NullPointerException {
            return this.dataSupplier.getResourceKeys(dataset);
        }

        public Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Resource dataset,
                                                              @SuppressWarnings("unused") Aspect aspect,
                                                              Collection<String> variables) {
            return this.dataSupplier.selectResourceValues(resource, dataset, variables);
        }

        public Map<Resource, Map<String, Set<RDFNode>>> selectResourceValues(Collection<Resource> resources,
                                                                             Resource dataset,
                                                                             @SuppressWarnings("unused") Aspect aspect,
                                                                             List<String> variables) {
            return this.dataSupplier.selectResourceValues(resources, dataset, variables);
        }


        @Override
        public Stream<List<Resource>> getCorrespondenceGroups() {
            return this.dataSupplier.getCorrespondenceGroups();
        }
    }
}
