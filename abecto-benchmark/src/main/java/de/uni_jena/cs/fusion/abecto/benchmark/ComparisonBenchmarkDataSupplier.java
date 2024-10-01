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
    private final double errorRate;
    private final RDFNode[] wrongValues;
    private int[] nextLocalIds;
    double[] overlapShare;

    public ComparisonBenchmarkDataSupplier(int sampleSize, int sampleCount, double pairwiseOverlap, double errorRate) {
        this.sampleSize = sampleSize;
        this.sampleCount = sampleCount;
        this.errorRate = errorRate;
        // generate wrong values per dataset
        // wrongValues[0] should not be used and exists to improve code readability by avoiding -1 shifts later on
        this.wrongValues = new RDFNode[sampleCount + 1];
        for (int sampleId = 0; sampleId <= sampleCount; sampleId++) {
            this.wrongValues[sampleId] = ResourceFactory.createTypedLiteral(sampleId);
        }
        calculateOverlapShares(sampleCount, pairwiseOverlap);
    }

    private void calculateOverlapShares(int sampleCount, double pairwiseOverlap) {
        overlapShare = new double[sampleCount + 1];
        // overlapShare[0] should not be used and exists to avoiding -1 shifts during access
        for (int overlappingSamplesCount = 1; overlappingSamplesCount <= sampleCount; overlappingSamplesCount++) {
            overlapShare[overlappingSamplesCount] =  overlapShare(overlappingSamplesCount, sampleCount, pairwiseOverlap);
        }
    }

    public Set<Resource> getDatasets() {
        return IntStream.range(0, sampleCount).mapToObj(sampleId -> ResourceFactory.createResource(Integer.toString(sampleId))).collect(Collectors.toSet());
    }

    public Stream<Resource> getResourceKeys(Resource sample) throws NullPointerException {
        int sampleId = Integer.parseInt(sample.getURI());
        return IntStream.range(0, sampleSize).map(localId -> localId + sampleId * sampleSize).mapToObj(i -> ResourceFactory.createResource(Integer.toString(i)));
    }

    public Set<RDFNode> getValueOfResource(Resource resource, Resource sample) {
        int resourceId = Integer.parseInt(resource.getURI());
        int sampleId = Integer.parseInt(sample.getURI());
        if (resourceId / sampleSize == sampleId) {
            return getValueOfResource(resourceId, sampleId);
        } else {
            return Collections.emptySet();
        }
    }

    public Set<RDFNode> getValueOfResource(int resourceId, int sampleId) {
        if (resourceId % sampleSize >= sampleSize * errorRate) {
            return Collections.singleton(correctValue);
        } else {
            return Collections.singleton(wrongValues[sampleId]);
        }
    }

    private double overlapShare(int overlappingSamplesCount, int sampleCount, double pairwiseOverlap) {
        return Math.pow(pairwiseOverlap, overlappingSamplesCount - 1) * Math.pow(1 - pairwiseOverlap,
                sampleCount - overlappingSamplesCount);
    }

    public Stream<List<Resource>> getCorrespondenceGroups() {
        double wrongValuesSubsetSize = sampleSize * errorRate;
        double correctValuesSubsetSize = sampleSize * (1 - errorRate);
        Stream<List<Resource>> correspondingWrongResources, correspondingCorrectResources;
        correspondingWrongResources = getCorrespondenceGroupsSubset(0, wrongValuesSubsetSize);
        correspondingCorrectResources = getCorrespondenceGroupsSubset((int) wrongValuesSubsetSize, correctValuesSubsetSize);
        return Stream.concat(correspondingWrongResources,correspondingCorrectResources);
    }

    /**
     * Generates one stream of corresponding resources per sample combination. The stream sizes are calculated
     * based on the sample size and the target overlap share between each pair of samples.
     *
     * @param localIdOffset    number of resource IDs to skip per sample
     * @param subsetSize       number of resources per sample in subset
     * @return one stream of corresponding resources per sample combination
     */
    private Stream<List<Resource>> getCorrespondenceGroupsSubset(int localIdOffset, double subsetSize) {
        setNextLocalIds(localIdOffset);

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
                int cases = (int) (getOverlapShare(coveredSamplesCount) * subsetSize);
                // generate stream of cases with covered samples
                correspondingResourcesStream[coveredSamplesBits] = Stream.generate(new CorrespondenceGroupSupplier(coveredSampleIds,
                        nextLocalIds)).limit(cases);
            } else {
                // empty stream for combinations without correspondences
                correspondingResourcesStream[coveredSamplesBits] = Stream.empty();
            }
        }
        return Streams.concat(correspondingResourcesStream);
    }

    private double getOverlapShare(int sampleCount) {
        return overlapShare[sampleCount];
    }

    private void setNextLocalIds(int nextLocalId) {
        nextLocalIds = new int[sampleCount];
        Arrays.fill(nextLocalIds, nextLocalId);
    }

    private int getNextLocalId(int sample) {
        return nextLocalIds[sample]++;
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
