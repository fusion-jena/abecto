package de.uni_jena.cs.fusion.abecto.processor;

import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class PopulationComparisonProcessorBenchmark {

    private class IndependentPopulationComparisonProcessor extends PopulationComparisonProcessor {

        private ComparisonBenchmarkDataSupplier dataSupplier;

        public IndependentPopulationComparisonProcessor(int populationSize, int datasetCount, double coverage) {
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

        public Stream<List<Resource>> getCorrespondenceGroups() {
            return this.dataSupplier.getCorrespondenceGroups();
        }
    }
}
