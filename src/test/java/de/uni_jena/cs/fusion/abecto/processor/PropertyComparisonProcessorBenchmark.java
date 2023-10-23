package de.uni_jena.cs.fusion.abecto.processor;

import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.*;
import java.util.stream.Stream;

public class PropertyComparisonProcessorBenchmark {

    private class IndependentPropertyComparisonProcessor extends PropertyComparisonProcessor {

        private ComparisonBenchmarkDataSupplier dataSupplier;

        public IndependentPropertyComparisonProcessor(int populationSize, int datasetCount, double coverage) {
            this.dataSupplier = new ComparisonBenchmarkDataSupplier(populationSize, datasetCount, coverage, 0);
        }

        @Override
        public Set<Resource> getDatasets() {
            return this.dataSupplier.getDatasets();
        }

        @Override
        public Stream<Resource> getResourceKeys(Aspect aspect, Resource dataset) throws NullPointerException {
            return this.dataSupplier.getResourceKeys(aspect, dataset);
        }

        public Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Resource dataset,
                                                              @SuppressWarnings("unused") Aspect aspect,
                                                              Collection<String> variables) {
            return this.dataSupplier.selectResourceValues(resource, dataset, aspect, variables);
        }

        public Map<Resource, Map<String, Set<RDFNode>>> selectResourceValues(Collection<Resource> resources,
                                                                             Resource dataset,
                                                                             @SuppressWarnings("unused") Aspect aspect,
                                                                             List<String> variables) {
            return this.dataSupplier.selectResourceValues(resources, dataset, aspect, variables);
        }


        @Override
        public Stream<List<Resource>> getCorrespondenceGroups() {
            return this.dataSupplier.getCorrespondenceGroups();
        }
    }
}
