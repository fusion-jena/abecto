package de.uni_jena.cs.fusion.abecto.processor;

import com.google.common.collect.Streams;
import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ComparisonBenchmarkDataSupplier extends PopulationComparisonProcessor {

    private final int populationSize, totalDatasetCount;
    private final double coverage, errorRate;
    private final RDFNode correctValue;
    private final RDFNode[] wrongValues;

    public ComparisonBenchmarkDataSupplier(int populationSize, int datasetCount, double coverage, double errorRate) {
        this.populationSize = populationSize;
        this.totalDatasetCount = datasetCount;
        this.coverage = coverage;
        this.errorRate = errorRate;
        // generate correct and wrong values per dataset
        this.correctValue = ResourceFactory.createTypedLiteral(-1);
        // wrongValues[0] should not be used and exists to improve code readability by avoiding -1 shifts later on
        this.wrongValues = new RDFNode[datasetCount + 1];
        for (int i = 0; i <= datasetCount; i++) {
            this.wrongValues[i] = ResourceFactory.createTypedLiteral(i);
        }
    }

    public Set<Resource> getDatasets() {
        return IntStream.range(0, totalDatasetCount).mapToObj(i -> ResourceFactory.createResource(Integer.toString(i))).collect(Collectors.toSet());
    }

    public Stream<Resource> getResourceKeys(@SuppressWarnings("unused") Aspect aspect, Resource dataset) throws NullPointerException {
        int datasetNumber = Integer.parseInt(dataset.getURI());
        return IntStream.range(0, populationSize).map(i -> i + datasetNumber * populationSize).mapToObj(i -> ResourceFactory.createResource(Integer.toString(i)));
    }

    public Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Resource dataset,
                                                          @SuppressWarnings("unused") Aspect aspect,
                                                          Collection<String> variables) {

        int resourceNumber = Integer.parseInt(resource.getURI());
        if (resourceNumber % populationSize >= populationSize * errorRate) {
            return Collections.singletonMap(variables.iterator().next(),
                    Collections.singleton(this.correctValue));
        } else {
            int datasetNumber = Integer.parseInt(dataset.getURI());
            return Collections.singletonMap(variables.iterator().next(),
                    Collections.singleton(this.wrongValues[datasetNumber]));
        }
    }

    public Map<Resource, Map<String, Set<RDFNode>>> selectResourceValues(Collection<Resource> resources,
                                                                         Resource dataset,
                                                                         @SuppressWarnings("unused") Aspect aspect, List<String> variables) {
        int datasetNumber = Integer.parseInt(dataset.getURI());

        Map<Resource, Map<String, Set<RDFNode>>> resourceValues = new HashMap<>();

        for (Resource resource : resources) {
            int resourceNumber = Integer.parseInt(resource.getURI());
            if (resourceNumber % populationSize >= populationSize * errorRate) {
                resourceValues.put(resource, Collections.singletonMap(variables.iterator().next(),
                        Collections.singleton(this.correctValue)));
            } else {
                resourceValues.put(resource, Collections.singletonMap(variables.iterator().next(),
                        Collections.singleton(this.wrongValues[datasetNumber])));
            }
        }
        return resourceValues;
    }

    private double overlapShare(int overlappingDatasetCount, int totalDatasetCount, double coverage) {
        return Math.pow(coverage, overlappingDatasetCount - 1) * Math.pow(1 - coverage, totalDatasetCount - overlappingDatasetCount);
    }


    public Stream<List<Resource>> getCorrespondenceGroups() {
        // precalculate overlap share, the share of an overlap of a given number of datasets on a dataset population
        // overlapShare[0] should not be used and exists to improve code readability by avoiding -1 shifts later on
        double[] overlapShare = new double[totalDatasetCount + 1];
        for (int overlappingDatasetCount = 1; overlappingDatasetCount < totalDatasetCount + 1; overlappingDatasetCount++) {
            overlapShare[overlappingDatasetCount] = overlapShare(overlappingDatasetCount, totalDatasetCount, coverage);
        }


        Stream<List<Resource>>[] errorCasesStreams = generateCasesStream(overlapShare, 0,
                (int) (populationSize * errorRate));
        Stream<List<Resource>>[] correctCasesStreams = generateCasesStream(overlapShare,
                (int) (populationSize * errorRate), (int) (populationSize * (1 - errorRate)));

        // join cases streams
        @SuppressWarnings("unchecked")
        Stream<List<Resource>>[] casesStreams = new Stream[(1 << totalDatasetCount) * 2];
        System.arraycopy(errorCasesStreams, 0, casesStreams, 0, 1 << totalDatasetCount);
        System.arraycopy(correctCasesStreams, 0, casesStreams, 1 << totalDatasetCount, 1 << totalDatasetCount);
        return Streams.concat(casesStreams);
    }

    /**
     * Generates one stream of corresponding resources per knowledge graph combination. The stream sizes are calculated
     * based on the total number of cases and the target overlap between each pair of knowledge graphs.
     *
     * @param overlapShare target overlap between each pair of knowledge graphs
     * @param idOffset     number of resource IDs to skip per knowledge graph
     * @param totalCases   number of resources to generate per knowledge graph
     *
     * @return one stream of corresponding resources per knowledge graph combination
     */
    private Stream<List<Resource>>[] generateCasesStream(double[] overlapShare, int idOffset, int totalCases) {

        int[] nextId = new int[totalDatasetCount];
        Arrays.fill(nextId, idOffset);

        @SuppressWarnings("unchecked")
        Stream<List<Resource>>[] casesStreams = new Stream[1 << totalDatasetCount];
        // iterate through all subsets represented by the bits of an int, 0 = not contained, 1 = contained
        for (int coveredDatasetsBits = 0; coveredDatasetsBits < 1 << totalDatasetCount /* = 2^{datasetCount}
         */; coveredDatasetsBits++) {
            int coveredDatasetsCount = Integer.bitCount(coveredDatasetsBits);


            if (coveredDatasetsCount >= 2) {
                // get array of numbers of covered datasets
                int[] coveredDatasets = new int[coveredDatasetsCount];
                int i = 0;
                for (int dataset = 0; dataset < totalDatasetCount; dataset++) {
                    if ((coveredDatasetsBits & (1 << dataset  /* = 2^{dataset} */)) != 0) {
                        coveredDatasets[i++] = dataset;
                    }
                }

                // calculate number of cases with covered datasets
                int cases = (int) (overlapShare[coveredDatasetsCount] * totalCases);
                // generate stream of cases with covered datasets
                casesStreams[coveredDatasetsBits] = Stream.generate(new CorrespondenceGroupSupplier(coveredDatasets, nextId)).limit(cases);
            } else {
                // empty stream for combinations without correspondences
                casesStreams[coveredDatasetsBits] = Stream.empty();
            }
        }
        return casesStreams;
    }

    private class CorrespondenceGroupSupplier implements Supplier<List<Resource>> {
        int[] coveredDatasets;
        int[] nextId;

        CorrespondenceGroupSupplier(int[] coveredDatasets, int[] nextId) {
            this.coveredDatasets = coveredDatasets;
            this.nextId = nextId;
        }

        @Override
        public List<Resource> get() {
            List<Resource> resources = Arrays.asList(new Resource[coveredDatasets.length]);
            for (int i = 0; i < coveredDatasets.length; i++) {
                int coveredDataset = coveredDatasets[i];
                resources.set(i, ResourceFactory.createResource(Integer.toString(nextId[coveredDataset]++ + coveredDataset * populationSize)));
            }
            return resources;
        }
    }
}
