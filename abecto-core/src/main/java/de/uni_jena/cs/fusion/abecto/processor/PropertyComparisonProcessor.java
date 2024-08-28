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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import de.uni_jena.cs.fusion.abecto.*;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetCount;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetPairCount;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetRatio;
import de.uni_jena.cs.fusion.abecto.measure.PerDatasetTupelRatio;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;

public class PropertyComparisonProcessor extends ComparisonProcessor<PropertyComparisonProcessor> {

    /**
     * Aspect to process.
     */
    @Parameter // TODO rename but keep name in configuration stable
    public Resource aspect;
    /**
     * Variables to process.
     */
    @Parameter
    public List<String> variables;
    /**
     * Language patterns to filter compared literals. Literals of datatype xsd:string and
     * rdf:langString will be considered only, if they match at least on of these patterns.
     * String literals without language tag will match with "", all string literals with
     * language tag match with "*". Default: "","*" (all match)
     */
    @Parameter
    public Collection<String> languageFilterPatterns = new ArrayList<>(List.of("", "*"));
    /**
     * If true, a literal of the type xsd:date and a literal of the type
     * xsd:dateTime with equal year, month and day part will match.
     */
    @Parameter
    public boolean allowTimeSkip;
    /**
     * If true, literals of the type xsd:string or rdf:langString with equal lexical
     * value but different language tag will match.
     */
    @Parameter
    public boolean allowLangTagSkip;
    Aspect theAspect; // TODO rename to `aspect` after renaming the aspect parameter variable into `aspectIri`
    Set<Resource> datasets;
    Set<ResourcePair> datasetPairsWithoutRepetition;
    Set<ResourcePair> datasetPairsWithRepetition;
    Set<ResourceTupel> datasetTupels;
    Map<Resource, Model> outputMetaModelByDataset;
    /**
     * Number of covered values of another dataset, per variable.
     */
    Map<String, PerDatasetPairCount> absoluteCoverage;
    Map<String, PerDatasetTupelRatio> relativeCoverage;
    /**
     * Number of values in this dataset, per variable.
     */
    Map<String, PerDatasetCount> count;
    /**
     * Number of distinct values in this dataset, per variable. Index: variable, affectedDataset
     */
    Map<String, PerDatasetCount> deduplicatedCount;
    Map<String, PerDatasetRatio> completeness;

    Map<Resource, Set<Resource>> uncoveredResourcesByDataset = new HashMap<>();


    @Override
    public final void run() {
        setAspect(aspect);
        setAspectDatasets();
        initializeMeasures();
        resetUncoveredResources();

        getCorrespondenceGroups().forEach(correspondingResources -> {
            Map<Resource, Set<Resource>> correspondingResourcesByDataset = separateByDataset(correspondingResources);
            removeFromUncoveredResources(correspondingResourcesByDataset);
            // get values for all corresponding resources in all datasets
            Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset = new HashMap<>();
            for (Resource dataset : datasets) {
                Set<Resource> correspondingResourcesOfDataset = correspondingResourcesByDataset.get(dataset);
                valuesByVariableByResourceByDataset.put(dataset, selectResourceValues(correspondingResourcesOfDataset, dataset, theAspect, variables));
                removeKnownWrongValues(valuesByVariableByResourceByDataset.get(dataset), dataset);
                removeExcludedValues(valuesByVariableByResourceByDataset.get(dataset));
                // increment count and deduplicated count
                for (String variable : variables) {
                    // get values of variable for all corresponding resources in dataset
                    var valuesOfCorrespondingResources = new ArrayList<RDFNode>();
                    valuesByVariableByResourceByDataset.get(dataset).values().stream()
                            .map(m -> m.getOrDefault(variable, Collections.emptySet()))
                            .forEach(valuesOfCorrespondingResources::addAll);

                    measureCountAndDeduplicatedCount(dataset, variable, valuesOfCorrespondingResources);
                }
            }

            for (ResourcePair datasetPair : datasetPairsWithRepetition) {
                for (String variable : variables) {
                    if (theAspect.getPattern(datasetPair.first).getResultVars().contains(variable)
                            && theAspect.getPattern(datasetPair.second).getResultVars().contains(variable)) {
                        calculateDeviationsAndOmissions(variable, datasetPair, valuesByVariableByResourceByDataset);
                    }
                }
            }
        });

        countAndDeduplicateValuesOfUncoveredResource();

        calculateCompleteness();
        calculateRelativeCoverage();

        storeMeasures();
    }

    private void setAspect(Resource aspect) {
        theAspect = this.getAspects().get(aspect);
    }

    private void setAspectDatasets() {
        datasets = theAspect.getDatasets();
        datasetPairsWithoutRepetition = ResourcePair.getPairsWithoutRepetitionOf(datasets);
        datasetPairsWithRepetition = ResourcePair.getPairsWithRepetitionOf(datasets);
        datasetTupels = ResourceTupel.getTupelsOf(datasets);
        outputMetaModelByDataset = getOutputMetaModels(datasets);
    }

    private void initializeMeasures() {
        initializeCount();
        initializeDeduplicatedCount();
        initializeAbsoluteCoverage();
        initializeRelativeCoverage();
        initializeCompleteness();
    }

    private void initializeCount() {
        count = new HashMap<>();
        for (String variable : variables) {
            PerDatasetCount countOfVariable = new PerDatasetCount(AV.count, OM.one);
            count.put(variable, countOfVariable);
        }
    }

    private void initializeDeduplicatedCount() {
        deduplicatedCount = new HashMap<>();
        for (String variable : variables) {
            PerDatasetCount deduplicatedCountOfVariable = new PerDatasetCount(AV.deduplicatedCount, OM.one);
            deduplicatedCount.put(variable, deduplicatedCountOfVariable);
        }
    }

    private void initializeAbsoluteCoverage() {
        absoluteCoverage = new HashMap<>();
        for (String variable : variables) {
            PerDatasetPairCount absoluteCoverageOfVariable = new PerDatasetPairCount(AV.absoluteCoverage, OM.one);
            absoluteCoverage.put(variable, absoluteCoverageOfVariable);
        }
    }

    private void initializeRelativeCoverage() {
        relativeCoverage = new HashMap<>();
        for (String variable : variables) {
            PerDatasetTupelRatio relativeCoverageOfVariable = new PerDatasetTupelRatio(AV.relativeCoverage, OM.one);
            relativeCoverage.put(variable, relativeCoverageOfVariable);
        }
    }

    private void initializeCompleteness() {
        completeness = new HashMap<>();
    }

    private void resetUncoveredResources() {
        uncoveredResourcesByDataset.clear();
        for (Resource dataset : datasets) {
            setResourcesOfDatasetAndAspectUncovered(dataset);
        }
    }

    Map<Resource, Set<Resource>> separateByDataset(List<Resource> resources) {
        Map<Resource, Set<Resource>> resourcesByDataset = new HashMap<>();
        for (Resource dataset : datasets) {
            Set<Resource> resourcesOfDataset = new HashSet<>(resources);
            resourcesOfDataset.retainAll(uncoveredResourcesByDataset.get(dataset));
            resourcesByDataset.put(dataset, resourcesOfDataset);
        }
        return resourcesByDataset;
    }

    void removeFromUncoveredResources(Map<Resource, Set<Resource>> coveredResourcesByDataset) {
        for (Resource dataset : coveredResourcesByDataset.keySet()) {
            Set<Resource> uncoveredResourcesOfDataset = uncoveredResourcesByDataset.get(dataset);
            Set<Resource> coveredResourcesOfDataset = coveredResourcesByDataset.get(dataset);
            uncoveredResourcesOfDataset.removeAll(coveredResourcesOfDataset);
        }
    }

    /**
     * Removes all values that are known wrong values.
     */
    private void removeKnownWrongValues(Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource, Resource dataset) {
        for (Resource resource : valuesByVariableByResource.keySet()) {
            removeKnownWrongValues(valuesByVariableByResource.get(resource), resource, dataset);
        }
    }

    /**
     * Removes all values that are known wrong values.
     */
    private void removeKnownWrongValues(Map<String, Set<RDFNode>> valuesByVariable, Resource resource, Resource dataset) {
        for (String variable : valuesByVariable.keySet()) {
            Set<RDFNode> values = valuesByVariable.get(variable);
            values.removeIf(value -> this.isWrongValue(resource, variable, value, dataset));
        }
    }

    protected boolean isWrongValue(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
                                   Resource affectedDataset) {
        return Metadata.isWrongValue(affectedResource, affectedVariableName, affectedValue, aspect,
                this.getInputMetaModelUnion(affectedDataset));
    }

    private void removeExcludedValues(Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource) {
        valuesByVariableByResource.forEach((resource, valuesByVariable) -> valuesByVariable.forEach((variable, values) -> values.removeIf(this::isExcludedValue)));
    }

    private void setResourcesOfDatasetAndAspectUncovered(Resource dataset) {
        Set<Resource> distinctResources = getResourceKeys(theAspect, dataset).collect(Collectors.toSet());
        uncoveredResourcesByDataset.put(dataset, distinctResources);
    }

    private void countAndDeduplicateValuesOfUncoveredResource() {
        for (Resource dataset : datasets) {
            Set<Resource> uncoveredResources = uncoveredResourcesByDataset.get(dataset);
            for (Resource uncoveredResource : uncoveredResources) {
                // TODO refactor

                // get resource values
                Map<String, Set<RDFNode>> valuesByVariable = selectResourceValues(uncoveredResource, dataset, theAspect, variables);

                // removeExcludedValues
                valuesByVariable.forEach((k, v) -> v.removeIf(this::isExcludedValue));

                for (String variable : valuesByVariable.keySet()) {
                    Collection<RDFNode> valuesOfVariable = valuesByVariable.get(variable);
                    measureCountAndDeduplicatedCount(dataset, variable, valuesOfVariable);
                }
            }
        }
    }

    void measureCountAndDeduplicatedCount(Resource dataset, String variable, Collection<RDFNode> valuesOfVariable) {
        long valuesCountWithDuplicates = valuesOfVariable.size();
        long valuesCountWithoutDuplicates = deduplicate(valuesOfVariable).size();
        count.get(variable).incrementByOrSet(dataset, valuesCountWithDuplicates);
        deduplicatedCount.get(variable).incrementByOrSet(dataset, valuesCountWithoutDuplicates);
    }

    private void calculateCompleteness() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            completeness.put(variable, calculateCompleteness(datasetPairsWithoutRepetition, absoluteCoverage.get(variable), deduplicatedCount.get(variable)));
        }
    }

    private void calculateRelativeCoverage() {
        for (String variable : variables) {
            PerDatasetTupelRatio relativeCoverageOfVariable = relativeCoverage.get(variable);
            PerDatasetPairCount absoluteCoverageOfVariable = absoluteCoverage.get(variable);
            PerDatasetCount deduplicatedCountOfVariable = deduplicatedCount.get(variable);
            relativeCoverageOfVariable.setRatioOf(absoluteCoverageOfVariable, deduplicatedCountOfVariable);
        }
    }

    private void storeMeasures() {
        storeCount();
        storeDeduplicatedCount();
        storeAbsoluteCoverage();
        storeRelativeCoverage();
        storeCompleteness();
    }

    private void storeCount() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            count.get(variable).storeInModelWithVariable(theAspect, variable, outputMetaModelByDataset);
        }
    }

    private void storeDeduplicatedCount() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            deduplicatedCount.get(variable).storeInModelWithVariable(theAspect, variable, outputMetaModelByDataset);
        }
    }

    private void storeAbsoluteCoverage() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            absoluteCoverage.get(variable).storeInModelWithVariable(theAspect, variable, outputMetaModelByDataset);
        }
    }

    private void storeRelativeCoverage() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            relativeCoverage.get(variable).storeInModelWithVariable(theAspect, variable, outputMetaModelByDataset);
        }
    }

    private void storeCompleteness() {
        for (String variable : variables) {
            // TODO add value exclusion filter description to measurement description
            completeness.get(variable).storeInModelWithVariableAsComparedToAllOtherResources(theAspect, variable, outputMetaModelByDataset);
        }
    }

    /**
     * Note: Not the most efficient way to do this, but there is no
     * {@link Comparator} available to use {@link TreeMap#TreeMap(Comparator)}.
     */
    private void mapResources(String variable, Map<RDFNode, Set<Resource>> resourcesByMappedValues,
                              Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource) {
        for (Resource resource : valuesByVariableByResource.keySet()) {
            for (RDFNode value : valuesByVariableByResource.get(resource).getOrDefault(variable,
                    Collections.emptySet())) {
                for (RDFNode valueKey : resourcesByMappedValues.keySet()) {
                    if (equivalentValues(value, valueKey)) {
                        Set<Resource> valuesSet = resourcesByMappedValues.get(valueKey);
                        // add values to existing set
                        valuesSet.add(resource);
                        // map value to same set, so equivalent values will share one set
                        resourcesByMappedValues.putIfAbsent(value, valuesSet);
                        break;
                    }
                }
                // no equivalent value in map
                resourcesByMappedValues.computeIfAbsent(value, v -> new HashSet<>()).add(resource);
            }
        }
    }

    /**
     * Compares the values of one variable from two corresponding resources in two
     * datasets and stores encountered deviations and issues in both according
     * outputMetaModels (derived by {@link #getOutputMetaModel(Resource)}). Either
     * the corresponding resources or the datasets might be equal, but not both at
     * once.
     *
     * @param variable Name of the compared variable
     */
    public void calculateDeviationsAndOmissions(String variable, ResourcePair datasetPair,
                                                Map<Resource, Map<Resource, Map<String, Set<RDFNode>>>> valuesByVariableByResourceByDataset) {

        Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource1 = valuesByVariableByResourceByDataset.get(datasetPair.first);
        Map<Resource, Map<String, Set<RDFNode>>> valuesByVariableByResource2 = valuesByVariableByResourceByDataset.get(datasetPair.second);

        // create common value-resource look-up
        Map<RDFNode, Set<Resource>> resourcesByMappedValues = new HashMap<>();
        mapResources(variable, resourcesByMappedValues, valuesByVariableByResource1);
        mapResources(variable, resourcesByMappedValues, valuesByVariableByResource2);


        // update measurements
        if (!datasetPair.first.equals(datasetPair.second)) {// do not measure for first == second
            // TODO test, that no absolute coverage exist for dataset compared with itself
            int pairwiseOverlap = getPairwiseOverlap(valuesByVariableByResource1.keySet(), valuesByVariableByResource2.keySet(), resourcesByMappedValues);
            absoluteCoverage.get(variable).incrementByOrSet(datasetPair, pairwiseOverlap);
        }

        // deviation: a pair of resources with each having a value not present in the
        // other resource
        // omission: a pair of resources with one having a value not present in the other,
        // but not vice versa

        for (Resource resource1 : valuesByVariableByResource1.keySet()) {
            var values1 = valuesByVariableByResource1.get(resource1).getOrDefault(variable, Collections.emptySet());
            for (Resource resource2 : valuesByVariableByResource2.keySet()) {
                var values2 = valuesByVariableByResource2.get(resource2).getOrDefault(variable, Collections.emptySet());

                var notMatchingValues1 = values1.stream().filter(value1 -> !resourcesByMappedValues
                        .getOrDefault(value1, Collections.emptySet()).contains(resource2)).toList();
                var notMatchingValues2 = values2.stream().filter(value2 -> !resourcesByMappedValues
                        .getOrDefault(value2, Collections.emptySet()).contains(resource1)).toList();

                // report missing not matching values
                if (notMatchingValues1.isEmpty()) {
                    for (RDFNode value2 : notMatchingValues2) {
                        Metadata.addValuesOmission(resource1, variable, datasetPair.second, resource2, value2, this.aspect,
                                this.getOutputMetaModel(datasetPair.first));
                    }
                } else if (notMatchingValues2.isEmpty()) {
                    for (RDFNode value1 : notMatchingValues1) {
                        Metadata.addValuesOmission(resource2, variable, datasetPair.first, resource1, value1, this.aspect,
                                this.getOutputMetaModel(datasetPair.second));
                    }
                } else {
                    // report pairs of deviating values
                    for (RDFNode value1 : notMatchingValues1) {
                        for (RDFNode value2 : notMatchingValues2) {
                            Metadata.addDeviation(resource1.asResource(), variable, value1, datasetPair.second,
                                    resource2.asResource(), value2, this.aspect, this.getOutputMetaModel(datasetPair.first));
                            Metadata.addDeviation(resource2.asResource(), variable, value2, datasetPair.first,
                                    resource1.asResource(), value1, this.aspect, this.getOutputMetaModel(datasetPair.second));
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if two values are equivalent.
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return {@code true}, if the values are equivalent, otherwise {@code false}
     */
    public boolean equivalentValues(RDFNode value1, RDFNode value2) {
        if (value1.isResource() && value2.isResource()) {
            return correspond(value1.asResource(), value2.asResource());
        } else if (value1.isLiteral() && value2.isLiteral()) {
            Literal literal1 = value1.asLiteral();
            Literal literal2 = value2.asLiteral();

            // same type/subtype check
            if (literal1.sameValueAs(literal2)) {
                return true;
            }

            RDFDatatype type1 = literal1.getDatatype();
            RDFDatatype type2 = literal2.getDatatype();

            // comparison of xsd:date and xsd:dateTime
            if (allowTimeSkip && (type1 instanceof XSDDateType && type2 instanceof XSDDateTimeType
                    || type1 instanceof XSDDateTimeType && type2 instanceof XSDDateType)) {
                XSDDateTime date1 = ((XSDDateTime) literal1.getValue());
                XSDDateTime date2 = ((XSDDateTime) literal2.getValue());
                return date1.getDays() == date2.getDays() //
                        && date1.getMonths() == date2.getMonths() //
                        && date1.getYears() == date2.getYears();
            }

            // ignore lang tags
            if (allowLangTagSkip && (type1 instanceof XSDBaseStringType || type1 instanceof RDFLangString)
                    && (type2 instanceof XSDBaseStringType || type2 instanceof RDFLangString)) {
                String string1 = literal1.getString();
                String string2 = literal2.getString();
                return Objects.equals(string1, string2);
            }

            // comparison of different number types
            try {
                BigDecimal decimal1, decimal2;

                // get precise BigDecimal of literal 1 and handle special cases of float/double
                if (type1 instanceof XSDBaseNumericType) {
                    decimal1 = new BigDecimal(literal1.getLexicalForm());
                } else if (type1 instanceof XSDDouble) {
                    double value1Double = literal1.getDouble();
                    // handle special cases
                    if (Double.isNaN(value1Double)) {
                        return type2 instanceof XSDFloat && Float.isNaN(literal2.getFloat());
                    } else if (value1Double == Double.NEGATIVE_INFINITY) {
                        return type2 instanceof XSDFloat && literal2.getFloat() == Float.NEGATIVE_INFINITY;
                    } else if (value1Double == Double.POSITIVE_INFINITY) {
                        return type2 instanceof XSDFloat && literal2.getFloat() == Float.POSITIVE_INFINITY;
                    }
                    // get value as BigDecimal
                    decimal1 = new BigDecimal(value1Double);
                    /*
                     * NOTE: don't use BigDecimal#valueOf(value1Double) or new
                     * BigDecimal(literal1.getLexicalForm()) to represented value from the double
                     * value space, not the double lexical space
                     */
                } else if (type1 instanceof XSDFloat) {
                    float value1Float = literal1.getFloat();
                    // handle special cases
                    if (Float.isNaN(value1Float)) {
                        return type2 instanceof XSDDouble && Double.isNaN(literal2.getDouble());
                    } else if (value1Float == Double.NEGATIVE_INFINITY) {
                        return type2 instanceof XSDDouble && literal2.getDouble() == Double.NEGATIVE_INFINITY;
                    } else if (value1Float == Double.POSITIVE_INFINITY) {
                        return type2 instanceof XSDDouble && literal2.getDouble() == Double.POSITIVE_INFINITY;
                    }
                    // get value as BigDecimal
                    decimal1 = new BigDecimal(value1Float);
                    /*
                     * NOTE: don't use BigDecimal#valueOf(value1Float) or new
                     * BigDecimal(literal1.getLexicalForm()) to represented value from the float
                     * value space, not the float lexical space
                     */
                } else {
                    return false;
                }

                // get precise BigDecimal of literal 2
                if (type2 instanceof XSDBaseNumericType) {
                    decimal2 = new BigDecimal(literal2.getLexicalForm());
                } else if (type2 instanceof XSDDouble) {
                    double value2Double = literal2.getDouble();
                    // handle special cases
                    if (Double.isNaN(value2Double) || value2Double == Double.NEGATIVE_INFINITY
                            || value2Double == Double.POSITIVE_INFINITY) {
                        return false;
                    }
                    // get value as BigDecimal
                    decimal2 = new BigDecimal(value2Double);
                    /*
                     * NOTE: don't use BigDecimal#valueOf(value2Double) or new
                     * BigDecimal(literal2.getLexicalForm()) to represented value from the double
                     * value space, not the double lexical space
                     */
                } else if (type2 instanceof XSDFloat) {
                    float value2Float = literal2.getFloat();
                    // handle special cases
                    if (Float.isNaN(value2Float) || value2Float == Float.NEGATIVE_INFINITY
                            || value2Float == Float.POSITIVE_INFINITY) {
                        return false;
                    }
                    // get value as BigDecimal
                    decimal2 = new BigDecimal(value2Float);
                    /*
                     * NOTE: don't use BigDecimal#valueOf(value2Float) or new
                     * BigDecimal(literal2.getLexicalForm()) to represented value from the float
                     * value space, not the float lexical space
                     */
                } else {
                    return false;
                }

                // compare BigDecimals
                return decimal1.compareTo(decimal2) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks if a value should be excluded from the comparison.
     *
     * @param value the value to check
     * @return {@code true}, if the value should not be used, otherwise {@code false}
     */
    public boolean isExcludedValue(RDFNode value) {
        if (!value.isLiteral() ||
                !(value.asLiteral().getDatatype() instanceof XSDBaseStringType) &&
                        !(value.asLiteral().getDatatype() instanceof RDFLangString)) {
            return false;
        } else {
            String langStr = value.asLiteral().getLanguage();
            return languageFilterPatterns.stream()
                    .noneMatch(languageFilterPattern -> NodeFunctions.langMatches(langStr, languageFilterPattern));
        }
    }

    /**
     * Returns a duplicate free list of the given values by removing all values equivalent to an earlier value.
     */
    private List<RDFNode> deduplicate(Iterable<RDFNode> values) {
        ArrayList<RDFNode> distinctValues = new ArrayList<>();
        for (RDFNode value : values) {
            if (distinctValues.stream().noneMatch(v -> equivalentValues(v, value))) {
                distinctValues.add(value);
            }
        }
        return distinctValues;
    }

    private <T> Set<T> distinctByIdentity(Collection<T> items) {
        Set<T> distinctItems = Collections.newSetFromMap(new IdentityHashMap<>());
        distinctItems.addAll(items);
        return distinctItems;
    }

    private int getPairwiseOverlap(Collection<Resource> resources1,
                                   Collection<Resource> resources2, Map<RDFNode, Set<Resource>> resourcesByMappedValues) {
        int pairwiseOverlap = 0;
        // use set of resource sets for mapping values
        // NOTE: equivalent values use the same set, so get distinct set instances
        for (Set<Resource> resourceSet : distinctByIdentity(resourcesByMappedValues.values())) {
            if (resources1.stream().noneMatch(resourceSet::contains)) {
                continue;
            }
            if (resources2.stream().noneMatch(resourceSet::contains)) {
                continue;
            }
            pairwiseOverlap++;
        }
        return pairwiseOverlap;
    }
}
