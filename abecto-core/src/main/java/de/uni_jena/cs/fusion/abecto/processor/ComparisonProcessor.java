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

package de.uni_jena.cs.fusion.abecto.processor;

import com.google.common.collect.Streams;
import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.ResourcePair;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetCount;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetPairCount;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetRatio;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Stream;

public abstract class ComparisonProcessor<P extends Processor<P>> extends Processor<P> {
    /**
     * Digits to preserve when rounding after division in measurement calculations.
     */
    public final static int SCALE = 16;

    /**
     * Returns a {@link Set} of all resource keys of a given aspect in a given
     * dataset.
     *
     * @param aspect  the {@link Aspect} the returned {@link Resource
     *                Resources} belong to
     * @param dataset the IRI of the source dataset of the {@link Resource
     *                Resources}
     * @return all resource keys of the given aspect in the given dataset
     * @throws NullPointerException if no pattern is defined for the given dataset
     */
    public Stream<Resource> getResourceKeys(Aspect aspect, Resource dataset)
            throws NullPointerException {

        Model datasetModels = this.getInputPrimaryModelUnion(dataset);

        Query aspectQuery = aspect.getPattern(dataset);
        // copy query without result vars
        Query query = new Query();
        query.setQuerySelectType();
        query.setPrefixMapping(aspectQuery.getPrefixMapping());
        query.setBase(aspectQuery.getBase());
        query.setQueryPattern(aspectQuery.getQueryPattern());
        if (aspectQuery.getValuesData() != null) {
            query.setValuesDataBlock(aspectQuery.getValuesVariables(), aspectQuery.getValuesData());
        }
        query.setLimit(aspectQuery.getLimit());
        query.setOffset(aspectQuery.getOffset());
        if (aspectQuery.getOrderBy() != null) {
            aspectQuery.getOrderBy()
                    .forEach(sortCondition -> query.addOrderBy(sortCondition.expression, sortCondition.direction));
        }
        aspectQuery.getGroupBy().forEachVarExpr(query::addGroupBy);
        aspectQuery.getHavingExprs().forEach(query::addHavingCondition);
        // set var
        query.addResultVar(aspect.getKeyVariable());
        query.setDistinct(true);

        ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();

        String keyVariableName = aspect.getKeyVariableName();

        return Streams.stream(results).map(querySolution -> querySolution.getResource(keyVariableName));
    }

    /**
     * Returns the values of the given {@link Resource} that are covered by the
     * pattern of the given dataset in the given {@link Model}. If this aspect does
     * not cover the given dataset or the model does not contain values for the
     * given resource, {@code null} is returned.
     */
    public Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Resource dataset,
                                                          Aspect aspect, Collection<String> variables) {
        if (!aspect.coversDataset(dataset)) {
            return Collections.emptyMap();
        }

        Model model = this.getInputPrimaryModelUnion(dataset);
        Query pattern = aspect.getPattern(dataset);
        Var keyVariable = aspect.getKeyVariable();

        return this.selectResourceValues(resource, pattern, keyVariable, variables, model);
    }

    /**
     * Returns the values of the given {@link Resource Resources} that are covered
     * by the pattern of the given dataset in the given {@link Model}. If this
     * aspect does not cover the given dataset an empty result is returned. If the
     * model does not contain any value for a given resource, the resource is mapped
     * to {@code null}.
     */
    public Map<Resource, Map<String, Set<RDFNode>>> selectResourceValues(Collection<Resource> resources,
                                                                         Resource dataset, Aspect aspect, List<String> variables) {
        if (!aspect.coversDataset(dataset)) {
            return Collections.emptyMap();
        }

        Model model = this.getInputPrimaryModelUnion(dataset);
        Query pattern = aspect.getPattern(dataset);
        Var keyVariable = aspect.getKeyVariable();

        Map<Resource, Map<String, Set<RDFNode>>> valuesByResource = new HashMap<>();

        for (Resource resource : resources) {
            Map<String, Set<RDFNode>> resourceValues = selectResourceValues(resource, pattern, keyVariable, variables, model);
            if (resourceValues != null) {
                valuesByResource.put(resource, resourceValues);
            }
        }

        return valuesByResource;
    }

    /**
     * Returns the values of the given {@link Resource} that are covered by the
     * pattern of the given dataset in the given {@link Model}. If this aspect does
     * not cover the given dataset or the model does not contain values for the
     * given resource, {@code null} is returned.
     */
    private Map<String, Set<RDFNode>> selectResourceValues(Resource resource, Query pattern, Var keyVariable,
                                                           Collection<String> variables, Model model) {
        Query query = SelectBuilder.rewrite(pattern.cloneQuery(),
                Collections.singletonMap(keyVariable, resource.asNode()));
        ResultSet results = QueryExecutionFactory.create(query, model).execSelect();

        if (!results.hasNext()) {
            return null;
        }

        Map<String, Set<RDFNode>> values = new HashMap<>();
        for (String variable : variables) {
            values.put(variable, new HashSet<>());
        }
        while (results.hasNext()) {
            QuerySolution result = results.next();
            for (String variable : variables) {
                if (result.contains(variable)) {
                    RDFNode value = result.get(variable);
                    values.get(variable).add(value);
                }
            }
        }

        return values;
    }

    Map<Resource, Model> getOutputMetaModels(Iterable<Resource> datasets) {
        Map<Resource, Model> outputMetaModelByDataset = new HashMap<>();
        for (Resource dataset : datasets) {
            outputMetaModelByDataset.put(dataset, getOutputMetaModel(dataset));
        }
        return outputMetaModelByDataset;
    }

    PerDatasetRatio calculateCompleteness(Iterable<ResourcePair> datasetPairs, PerDatasetPairCount absoluteCoverage, PerDatasetCount deduplicatedCount) {
        PerDatasetRatio completeness = new PerDatasetRatio(AV.marCompletenessThomas08, OM.one);
        long totalPairwiseOverlap = calculateTotalPairwiseOverlap(datasetPairs, absoluteCoverage);
        if (totalPairwiseOverlap != 0) {
            BigDecimal estimatedPopulationSize = calculateEstimatedPopulationSize(datasetPairs, deduplicatedCount, totalPairwiseOverlap);
            completeness.setRatioOf(deduplicatedCount, estimatedPopulationSize);
        }
        return completeness;
    }

    long calculateTotalPairwiseOverlap(Iterable<ResourcePair> datasetPairs, PerDatasetPairCount absoluteCoverage) {
        long totalPairwiseOverlap = 0L;
        for (ResourcePair datasetPair : datasetPairs) {
            if (absoluteCoverage.contains(datasetPair)) {
                totalPairwiseOverlap += absoluteCoverage.get(datasetPair);
            }
        }
        return totalPairwiseOverlap;
    }

    BigDecimal calculateEstimatedPopulationSize(Iterable<ResourcePair> datasetPairs, PerDatasetCount deduplicatedCount, long totalPairwiseOverlap) {
        BigDecimal estimatedPopulationSize = BigDecimal.ZERO;
        for (ResourcePair datasetPair : datasetPairs) {
            BigDecimal deduplicatedCount1 = BigDecimal.valueOf(deduplicatedCount.get(datasetPair.first));
            BigDecimal deduplicatedCount2 = BigDecimal.valueOf(deduplicatedCount.get(datasetPair.second));
            estimatedPopulationSize = estimatedPopulationSize.add(deduplicatedCount1.multiply(deduplicatedCount2));
        }
        estimatedPopulationSize = estimatedPopulationSize.divide(BigDecimal.valueOf(totalPairwiseOverlap), SCALE,
                RoundingMode.HALF_UP);
        return estimatedPopulationSize;
    }
}
