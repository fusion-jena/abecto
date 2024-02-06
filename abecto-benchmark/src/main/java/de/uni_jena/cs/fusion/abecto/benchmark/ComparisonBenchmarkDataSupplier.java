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

import com.google.common.collect.Streams;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ComparisonBenchmarkDataSupplier {

    protected static final RDFNode correctValue = ResourceFactory.createTypedLiteral(-1);
    private final int sampleSize, sampleCount;
    private final double pairwiseOverlap, errorRate;
    private final RDFNode[] wrongValues;

    public ComparisonBenchmarkDataSupplier(int sampleSize, int sampleCount, double pairwiseOverlap, double errorRate) {
        this.sampleSize = sampleSize;
        this.sampleCount = sampleCount;
        this.pairwiseOverlap = pairwiseOverlap;
        this.errorRate = errorRate;
        // generate wrong values per dataset
        // wrongValues[0] should not be used and exists to improve code readability by avoiding -1 shifts later on
        this.wrongValues = new RDFNode[sampleCount + 1];
        for (int sampleId = 0; sampleId <= sampleCount; sampleId++) {
            this.wrongValues[sampleId] = ResourceFactory.createTypedLiteral(sampleId);
        }
    }

    public Set<Resource> getDatasets() {
        return IntStream.range(0, sampleCount).mapToObj(sampleId -> ResourceFactory.createResource(Integer.toString(sampleId))).collect(Collectors.toSet());
    }

    public Stream<Resource> getResourceKeys(Resource sample) throws NullPointerException {
        int sampleId = Integer.parseInt(sample.getURI());
        return IntStream.range(0, sampleSize).map(localId -> localId + sampleId * sampleSize).mapToObj(i -> ResourceFactory.createResource(Integer.toString(i)));
    }

    public Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Resource sample,
                                                          Collection<String> variables) {
        int resourceId = Integer.parseInt(resource.getURI());
        int sampleId = Integer.parseInt(sample.getURI());
        if (resourceId / sampleSize == sampleId) {
            return selectResourceValues(resourceId, sampleId, variables.iterator().next());
        } else {
            return null;
        }
    }

    public Map<String, Set<RDFNode>> selectResourceValues(int resourceId, int sampleId,
                                                          String variable) {
        Set<RDFNode> valuesSet = new HashSet<>(1); // Note: must be mutable
        if (resourceId % sampleSize >= sampleSize * errorRate) {
            valuesSet.add(correctValue);
        } else {
            valuesSet.add(this.wrongValues[sampleId]);
        }
        return Collections.singletonMap(variable, valuesSet);
    }

    public Map<Resource, Map<String, Set<RDFNode>>> selectResourceValues(Collection<Resource> resources,
                                                                         Resource sample, List<String> variables) {
        int sampleId = Integer.parseInt(sample.getURI());
        String variable = variables.iterator().next();
        Map<Resource, Map<String, Set<RDFNode>>> resourceValues = new HashMap<>();
        for (Resource resource : resources) {
            int resourceId = Integer.parseInt(resource.getURI());
            if (resourceId / sampleSize == sampleId) {
                resourceValues.put(resource, selectResourceValues(resourceId, sampleId, variable));
            }
        }
        return resourceValues;
    }

    private double overlapShare(int overlappingSamplesCount, int sampleCount, double pairwiseOverlap) {
        return Math.pow(pairwiseOverlap, overlappingSamplesCount - 1) * Math.pow(1 - pairwiseOverlap,
                sampleCount - overlappingSamplesCount);
    }


    public Stream<List<Resource>> getCorrespondenceGroups() {
        // precalculate overlap share, the share of an overlap of a given number of datasets on a dataset population
        // overlapShare[0] should not be used and exists to improve code readability by avoiding -1 shifts later on
        double[] overlapShare = new double[sampleCount + 1];
        for (int overlappingSamplesCount = 1; overlappingSamplesCount < sampleCount + 1; overlappingSamplesCount++) {
            overlapShare[overlappingSamplesCount] = overlapShare(overlappingSamplesCount, sampleCount, pairwiseOverlap);
        }


        Stream<List<Resource>>[] correspondingWrongResourcesStream = generateCasesStream(overlapShare, 0, sampleSize, errorRate);
        Stream<List<Resource>>[] correspondingCorrectResourcesStream = generateCasesStream(overlapShare,
                (int) (sampleSize * errorRate), sampleSize, 1 - errorRate);

        // join cases streams
        @SuppressWarnings("unchecked") Stream<List<Resource>>[] correspondingResourcesStream = new Stream[(1 << sampleCount) * 2];
        System.arraycopy(correspondingWrongResourcesStream, 0, correspondingResourcesStream, 0, 1 << sampleCount);
        System.arraycopy(correspondingCorrectResourcesStream, 0, correspondingResourcesStream, 1 << sampleCount, 1 << sampleCount);
        return Streams.concat(correspondingResourcesStream);
    }

    /**
     * Generates one stream of corresponding resources per sample combination. The stream sizes are calculated
     * based on the sample size and the target overlap share between each pair of samples.
     *
     * @param overlapShare     target overlap share between each pair of sample
     * @param localIdOffset    number of resource IDs to skip per sample
     * @param sampleSize       number of resources per sample
     * @param correctnessShare share of resources per sample due to correctness per sample
     * @return one stream of corresponding resources per sample combination
     */
    private Stream<List<Resource>>[] generateCasesStream(double[] overlapShare, int localIdOffset, int sampleSize, double correctnessShare) {

        int[] nextLocalId = new int[sampleCount];
        Arrays.fill(nextLocalId, localIdOffset);

        @SuppressWarnings("unchecked") Stream<List<Resource>>[] correspondingResourcesStream = new Stream[1 << sampleCount];
        // iterate through all subsets represented by the bits of an int, 0 = not contained, 1 = contained
        int subsetCount = 1 << sampleCount; // = 2^{sampleCount}
        for (int coveredSamplesBits = 0; coveredSamplesBits < subsetCount; coveredSamplesBits++) {
            int coveredSamplesCount = Integer.bitCount(coveredSamplesBits);


            if (coveredSamplesCount >= 2) {
                // get array of covered samples ids
                int[] coveredSampleIds = new int[coveredSamplesCount];
                for (int sampleId = 0, i = 0; sampleId < sampleCount; sampleId++) {
                    if ((coveredSamplesBits & (1 << sampleId  /* = 2^{sampleId} */)) != 0) {
                        coveredSampleIds[i++] = sampleId;
                    }
                }

                // calculate number of cases with covered samples
                int cases = (int) (overlapShare[coveredSamplesCount] * sampleSize * correctnessShare);
                // generate stream of cases with covered samples
                correspondingResourcesStream[coveredSamplesBits] = Stream.generate(new CorrespondenceGroupSupplier(coveredSampleIds,
                        nextLocalId)).limit(cases);
            } else {
                // empty stream for combinations without correspondences
                correspondingResourcesStream[coveredSamplesBits] = Stream.empty();
            }
        }
        return correspondingResourcesStream;
    }

    private class CorrespondenceGroupSupplier implements Supplier<List<Resource>> {
        int[] coveredSampleIds;
        int[] nextLocalId;

        CorrespondenceGroupSupplier(int[] coveredSampleIds, int[] nextLocalId) {
            this.coveredSampleIds = coveredSampleIds;
            this.nextLocalId = nextLocalId;
        }

        @Override
        public List<Resource> get() {
            List<Resource> resources = Arrays.asList(new Resource[coveredSampleIds.length]);
            for (int i = 0; i < coveredSampleIds.length; i++) {
                int sampleId = coveredSampleIds[i];
                resources.set(i,
                        ResourceFactory.createResource(Integer.toString(nextLocalId[sampleId]++ + sampleId * sampleSize)));
            }
            return resources;
        }
    }
}
