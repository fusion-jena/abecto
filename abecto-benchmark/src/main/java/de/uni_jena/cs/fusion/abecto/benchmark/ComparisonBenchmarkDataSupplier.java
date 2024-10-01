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
        this.wrongValues = new RDFNode[sampleCount];
        for (int sampleId = 0; sampleId < sampleCount; sampleId++) {
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

    private Stream<List<Resource>> getCorrespondenceGroupsSubset(int localIdOffset, double subsetSize) {
        Stream<List<Resource>> correspondenceGroupsSubset = Stream.empty();
        setNextLocalIds(localIdOffset);

        int sampleCombinationsCount = twoToThePowerOf(sampleCount);
        // iterate through all subsets represented by the bits of an int: 0 = not contained, 1 = contained
        for (int overlappingSamplesBits = 0; overlappingSamplesBits < sampleCombinationsCount; overlappingSamplesBits++) {
            Stream<List<Resource>> correspondenceGroupsSubsetOfCombination = getCorrespondenceGroupsSubsetOfCombination(overlappingSamplesBits, subsetSize);
            correspondenceGroupsSubset = Stream.concat(correspondenceGroupsSubset, correspondenceGroupsSubsetOfCombination);
        }
        return correspondenceGroupsSubset;
    }

    private void setNextLocalIds(int nextLocalId) {
        nextLocalIds = new int[sampleCount];
        Arrays.fill(nextLocalIds, nextLocalId);
    }

    private int twoToThePowerOf(int exponent) {
        return 1 << exponent;
    }

    private Stream<List<Resource>> getCorrespondenceGroupsSubsetOfCombination(int overlappingSamplesBits, double subsetSize) {
        int[] overlappingSamplesIds = overlappingSamples(overlappingSamplesBits);
        int overlappingSamplesCount = overlappingSamplesIds.length;

        if (overlappingSamplesCount >= 2) {
            int overlapSize = (int) (getOverlapShare(overlappingSamplesCount) * subsetSize);
            int[] localIdsOffsetOfOverlap = nextLocalIds.clone();
            incrementNextIdsOfSamples(overlappingSamplesIds,overlapSize);
            return Stream.generate(new CorrespondenceGroupSupplier(overlappingSamplesIds, localIdsOffsetOfOverlap, sampleSize)).limit(overlapSize);
        } else {
            return Stream.empty();
        }
    }

    private int[] overlappingSamples(int overlappingSamplesBits) {
        int overlappingSamplesCount = Integer.bitCount(overlappingSamplesBits);
        int[] overlappingSamplesIds = new int[overlappingSamplesCount];
        for (int sampleId = 0, i = 0; sampleId < sampleCount; sampleId++) {
            if ((overlappingSamplesBits & twoToThePowerOf(sampleId)) != 0) {
                overlappingSamplesIds[i++] = sampleId;
            }
        }
        return overlappingSamplesIds;
    }

    private double getOverlapShare(int sampleCount) {
        return overlapShare[sampleCount];
    }

    private void incrementNextIdsOfSamples(int[] sampleIds, int incrementBy) {
        for (int sampleId: sampleIds) {
            nextLocalIds[sampleId] += incrementBy;
        }
    }

    private static class CorrespondenceGroupSupplier implements Supplier<List<Resource>> {
        int[] nextLocalIds;
        int[] overlappingSamplesIds;
        int overlappingSamplesCount;
        int sampleSize;

        CorrespondenceGroupSupplier(int[] overlappingSamplesIds, int[] nextLocalIds, int sampleSize) {
            this.nextLocalIds = nextLocalIds;
            this.overlappingSamplesIds = overlappingSamplesIds;
            overlappingSamplesCount = overlappingSamplesIds.length;
            this.sampleSize = sampleSize;
        }

        @Override
        public List<Resource> get() {
            List<Resource> resources = Arrays.asList(new Resource[overlappingSamplesCount]);
            for (int i = 0; i < overlappingSamplesCount; i++) {
                int sampleId = overlappingSamplesIds[i];
                Resource nextResourceOfSample = getNextResourceOfSample(sampleId);
                resources.set(i, nextResourceOfSample);
            }
            return resources;
        }

        private Resource getNextResourceOfSample(int sampleId) {
            int nextLocalId = getNextLocalId(sampleId);
            int resourceId = nextLocalId + sampleId * sampleSize;
            return ResourceFactory.createResource(Integer.toString(resourceId));
        }

        private int getNextLocalId(int sampleId) {
            return nextLocalIds[sampleId]++;
        }
    }
}
