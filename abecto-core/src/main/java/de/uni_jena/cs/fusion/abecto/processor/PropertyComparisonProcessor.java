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

import java.util.*;
import java.util.stream.Collectors;

import de.uni_jena.cs.fusion.abecto.*;
import de.uni_jena.cs.fusion.abecto.measure.*;
import de.uni_jena.cs.fusion.abecto.util.Literals;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.sparql.core.Var;
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
    Set<ResourcePair> datasetPairs;
    Map<Resource, Model> outputMetaModelByDataset;

    Map<String, AbsoluteCoverage> absoluteValueCoverage;
    Map<String, RelativeCoverage> relativeValueCoverage = new HashMap<>();
    Map<String, Count> nonDistinctValuesCount;
    Map<String, DeduplicatedCount> distinctValuesCount;
    Map<String, DuplicateCount> duplicateValuesCount = new HashMap<>();
    Map<String, AbsoluteCoveredness> absoluteValueCoveredness;
    Map<String, RelativeCoveredness> relativeValueCoveredness;
    Map<String, Completeness> valueCompleteness = new HashMap<>();

    Map<Resource, Set<Resource>> unprocessedResourcesByDataset = new HashMap<>();
    Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> resourcesByNonDistinctValueByDatasetByVariable = new HashMap<>();
    Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> resourcesByDistinctValueByDatasetByVariable = new HashMap<>();
    Map<Resource, Set<Resource>> correspondingResourcesByDataset = new HashMap<>();

    @Override
    public final void run() {
        setAspect(aspect);
        setAspectDatasets();
        initializeMeasures();
        loadResourcesOfAspect();
        compareValuesOfCorrespondingResources();
        compareValuesOfNotCorrespondingResources();
        calculateDuplicateCount();
        calculateRelativeCoveredness();
        calculateRelativeCoverage();
        calculateCompleteness();
        storeMeasures();
    }

    protected void setAspect(Resource aspect) {
        theAspect = this.getAspects().get(aspect);
    }

    protected void setAspectDatasets() {
        datasets = theAspect.getDatasets();
        datasetPairs = ResourcePair.getPairsOf(datasets);
        outputMetaModelByDataset = getOutputMetaModels(datasets);
        resourcesByNonDistinctValueByDatasetByVariable = createMapOfResourcesByValueByDatasetByVariable();
        resourcesByDistinctValueByDatasetByVariable = createMapOfResourcesByValueByDatasetByVariable();
    }

    protected Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> createMapOfResourcesByValueByDatasetByVariable() {
        Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> map = new HashMap<>();
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByValueByDataset = new HashMap<>();
            map.put(variable, resourcesByValueByDataset);
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    resourcesByValueByDataset.put(dataset, new HashMap<>());
                }
            }
        }
        return map;
    }

    protected void initializeMeasures() {
        String valueFilterCondition = getLanguageFilterCondition();
        nonDistinctValuesCount = Measure.createMapByVariable(variables, valueFilterCondition, Count.class);
        setZeroForVariablesCoveredByDataset(nonDistinctValuesCount);
        distinctValuesCount = Measure.createMapByVariable(variables, valueFilterCondition, DeduplicatedCount.class);
        absoluteValueCoverage = Measure.createMapByVariable(variables, valueFilterCondition, AbsoluteCoverage.class);
        setZeroForVariablesCoveredByDatasetPair(absoluteValueCoverage);
        absoluteValueCoveredness = Measure.createMapByVariable(variables, valueFilterCondition, AbsoluteCoveredness.class);
        setZeroForVariablesCoveredByDataset(absoluteValueCoveredness);
        relativeValueCoveredness = Measure.createMapByVariable(variables, valueFilterCondition, RelativeCoveredness.class);
    }

    protected String getLanguageFilterCondition() {
        String prefix = "langMatches(lang(?value), \"";
        String suffix = "\")";
        String delimiter = suffix + " || " + prefix;
        return prefix + String.join(delimiter, languageFilterPatterns) + suffix;
    }

    protected <M extends LongMeasure<Resource>> void setZeroForVariablesCoveredByDataset(Map<String, M> measures) {
        for (String variable : variables) {
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    measures.get(variable).setZero(dataset);
                }
            }
        }
    }

    protected <M extends LongMeasure<ResourcePair>> void setZeroForVariablesCoveredByDatasetPair(Map<String, M> measures) {
        for (String variable : variables) {
            for (ResourcePair datasetPair : datasetPairs) {
                if (theAspect.variableCoveredByDatasets(variable, datasetPair.first, datasetPair.second)) {
                    measures.get(variable).setZero(datasetPair);
                }
            }
        }
    }

    protected void loadResourcesOfAspect() {
        for (Resource dataset : datasets) {
            loadResourcesOfAspectAndDataset(dataset);
        }
    }

    protected void loadResourcesOfAspectAndDataset(Resource dataset) {
        Set<Resource> resourcesOfDataset = getResourceKeys(theAspect, dataset).collect(Collectors.toSet());
        unprocessedResourcesByDataset.put(dataset, resourcesOfDataset);
    }

    protected void compareValuesOfCorrespondingResources() {
        getCorrespondenceGroups().forEach(this::compareValuesOfCorrespondingResources);
    }

    protected void compareValuesOfCorrespondingResources(List<Resource> correspondingResources) {
        setCorrespondingResourcesByDataset(correspondingResources);
        removeFromUnprocessedResources(correspondingResources);

        loadNonDistinctValues();
        calculateDistinctValues();

        measureNonDistinctValuesCount();
        measureDistinctValuesCount();
        measureAbsoluteCoverageAndAbsoluteCoveredness();
        reportDeviationsAndOmissions();
    }

    protected void setCorrespondingResourcesByDataset(Collection<Resource> correspondingResources) {
        for (Resource dataset : datasets) {
            Set<Resource> correspondingResourcesOfDataset = new HashSet<>(correspondingResources);
            correspondingResourcesOfDataset.retainAll(unprocessedResourcesByDataset.get(dataset));
            correspondingResourcesByDataset.put(dataset,correspondingResourcesOfDataset);
        }
    }

    protected void removeFromUnprocessedResources(Collection<Resource> correspondingResources) {
        for (Set<Resource> unprocessedResourcesOfDataset : unprocessedResourcesByDataset.values()) {
            unprocessedResourcesOfDataset.removeAll(correspondingResources);
        }
    }

    protected void loadNonDistinctValues() {
        clearValues(resourcesByNonDistinctValueByDatasetByVariable);

        Set<Resource> datasetsOfCorrespondingResources = correspondingResourcesByDataset.keySet();

        for (Resource dataset : datasetsOfCorrespondingResources) {
            Collection<Resource> resourcesOfDataset = correspondingResourcesByDataset.get(dataset);
            loadNonDistinctValuesOfDataset(dataset, resourcesOfDataset);
        }
    }

    protected void clearValues(Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> resourcesByNonDistinctValueByDatasetByVariable) {
        for (Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByNonDistinctValueByDataset : resourcesByNonDistinctValueByDatasetByVariable.values()) {
            for (Map<RDFNode, Set<Resource>> resourcesByNonDistinctValue : resourcesByNonDistinctValueByDataset.values()) {
                resourcesByNonDistinctValue.clear();
            }
        }
    }

    protected void loadNonDistinctValuesOfDataset(Resource dataset, Collection<Resource> resourcesOfDataset) {
        for (Resource resource : resourcesOfDataset) {
            Map<String, Set<RDFNode>> valuesByVariable = getValuesByVariable(dataset, resource);
            for (String variable : variables) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    Iterable<RDFNode> values = valuesByVariable.get(variable);
                    Map<RDFNode, Set<Resource>> resourcesByNonDistinctValue = resourcesByNonDistinctValueByDatasetByVariable.get(variable).get(dataset);
                    for (RDFNode value : values) {
                        Set<Resource> resourcesOfValue = resourcesByNonDistinctValue.computeIfAbsent(value, k -> new HashSet<>());
                        resourcesOfValue.add(resource);
                    }
                }
            }
        }
    }

    protected void calculateDistinctValues() {
        for (String variable : variables) {
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    calculateDistinctValuesForVariableAndDataset(variable, dataset);
                }
            }
        }
    }

    protected void calculateDistinctValuesForVariableAndDataset(String variable, Resource dataset) {
        Map<RDFNode, Set<Resource>> resourcesByNonDistinctValue = resourcesByNonDistinctValueByDatasetByVariable.get(variable).get(dataset);
        Map<RDFNode, Set<Resource>> resourcesByDistinctValue = resourcesByDistinctValueByDatasetByVariable.get(variable).get(dataset);
        resourcesByDistinctValue.clear();
        for (RDFNode nonDistinctValue : resourcesByNonDistinctValue.keySet()) {
            Set<Resource> resourcesOfNonDistinctValue = resourcesByNonDistinctValue.get(nonDistinctValue);
            Set<Resource> resourcesOfDistinctValue = getResourcesOfEquivalentDistinctValue(nonDistinctValue, resourcesByDistinctValue);
            resourcesOfDistinctValue.addAll(resourcesOfNonDistinctValue);
        }
    }

    Set<Resource> getResourcesOfEquivalentDistinctValue(RDFNode nonDistinctValue, Map<RDFNode, Set<Resource>> resourcesByDistinctValue) {
        Set<Resource> resourcesOfDistinctValue;
        for (RDFNode distinctValue : resourcesByDistinctValue.keySet()) {
            if (equivalentValues(nonDistinctValue, distinctValue)) {
                resourcesOfDistinctValue = resourcesByDistinctValue.get(distinctValue);
                return resourcesOfDistinctValue;
            }
        }
        resourcesOfDistinctValue = new HashSet<>();
        resourcesByDistinctValue.put(nonDistinctValue, resourcesOfDistinctValue);
        return resourcesOfDistinctValue;
    }

    /**
     * Checks if two values are equivalent.
     *
     * @param value1 the first value to compare
     * @param value2 the second value to compare
     * @return {@code true}, if the values are equivalent, otherwise {@code false}
     */
    protected boolean equivalentValues(RDFNode value1, RDFNode value2) {
        if (value1.isResource() && value2.isResource()) {
            return correspond(value1.asResource(), value2.asResource());
        }
        if (value1.isLiteral() && value2.isLiteral()) {
            Literal literal1 = value1.asLiteral();
            Literal literal2 = value2.asLiteral();
            return Literals.equivalentLiteralsOfSameTypes(literal1, literal2) ||
                    allowLangTagSkip && Literals.equivalentStringsIgnoringLangTag(literal1, literal2) ||
                    allowTimeSkip && Literals.equivalentDatesIgnoringTimes(literal1, literal2) ||
                    Literals.equivalentNumbersIgnoringNumberType(literal1, literal2);
        }
        return false;
    }

    protected void measureNonDistinctValuesCount() {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByNonDistinctValueByDataset = resourcesByNonDistinctValueByDatasetByVariable.get(variable);
            PerDatasetLongMeasure nonDistinctValuesCountOfVariable = nonDistinctValuesCount.get(variable);
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    Map<RDFNode, Set<Resource>> resourcesByNonDistinctValue = resourcesByNonDistinctValueByDataset.get(dataset);
                    for (RDFNode nonDistinctValue : resourcesByNonDistinctValue.keySet()) {
                        Set<Resource> resourcesOfNonDistinctValue = resourcesByNonDistinctValue.get(nonDistinctValue);
                        nonDistinctValuesCountOfVariable.incrementByOrSet(dataset, resourcesOfNonDistinctValue.size());
                    }
                }
            }
        }
    }

    protected void measureDistinctValuesCount() {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByDistinctValueByDataset = resourcesByDistinctValueByDatasetByVariable.get(variable);
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    Map<RDFNode, Set<Resource>> resourcesByDistinctValue = resourcesByDistinctValueByDataset.get(dataset);
                    int coveredDistinctValuesCountOfDataset = resourcesByDistinctValue.size();
                    distinctValuesCount.get(variable).incrementByOrSet(dataset, coveredDistinctValuesCountOfDataset);
                }
            }
        }
    }

    protected void measureAbsoluteCoverageAndAbsoluteCoveredness() {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByDistinctValueByDataset = resourcesByDistinctValueByDatasetByVariable.get(variable);
            AbsoluteCoverage absoluteCoverageForVariable = absoluteValueCoverage.get(variable);
            AbsoluteCoveredness absoluteCoverednessForVariable = absoluteValueCoveredness.get(variable);
            Map<Resource, Set<RDFNode>> uncoveredDistinctValuesByDataset = new HashMap<>();
            for (Resource dataset: datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    Set<RDFNode> uncoveredDistinctValuesOfDataset = new HashSet<>(resourcesByDistinctValueByDataset.get(dataset).keySet());
                    uncoveredDistinctValuesByDataset.put(dataset, uncoveredDistinctValuesOfDataset);
                    absoluteCoverednessForVariable.incrementByOrSet(dataset, uncoveredDistinctValuesOfDataset.size());
                }
            }
            for (ResourcePair datasetPair : datasetPairs) {
                if (theAspect.variableCoveredByDatasets(variable, datasetPair.first, datasetPair.second)) {
                    Set<RDFNode> distinctValuesOfFirstDataset = resourcesByDistinctValueByDataset.get(datasetPair.first).keySet();
                    Set<RDFNode> distinctValuesOfSecondDataset = resourcesByDistinctValueByDataset.get(datasetPair.second).keySet();
                    Set<RDFNode> uncoveredDistinctValuesOfFirstDataset = uncoveredDistinctValuesByDataset.get(datasetPair.first);
                    Set<RDFNode> uncoveredDistinctValuesOfSecondDataset = uncoveredDistinctValuesByDataset.get(datasetPair.second);
                    for (RDFNode valueOfFirstDataset : distinctValuesOfFirstDataset) {
                        for (RDFNode valueOfSecondDataset : distinctValuesOfSecondDataset) {
                            if (equivalentValues(valueOfFirstDataset, valueOfSecondDataset)) {
                                absoluteCoverageForVariable.incrementByOrSetOne(datasetPair);
                                uncoveredDistinctValuesOfFirstDataset.remove(valueOfFirstDataset);
                                uncoveredDistinctValuesOfSecondDataset.remove(valueOfSecondDataset);
                                break;
                            }
                        }
                    }
                }
            }
            for (Resource dataset: datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    Set<RDFNode> uncoveredDistinctValuesOfDataset = uncoveredDistinctValuesByDataset.get(dataset);
                    absoluteCoverednessForVariable.decrementByOrSet(dataset, uncoveredDistinctValuesOfDataset.size());
                }
            }
        }
    }

    protected void reportDeviationsAndOmissions() {
        for (ResourcePair datasetPair : datasetPairs) {
            reportDeviationsAndOmissionsForDatasetPair(datasetPair);
        }
        for (Resource dataset : datasets) {
            ResourcePair datasetPair = ResourcePair.getPair(dataset, dataset);
            reportDeviationsAndOmissionsForDatasetPair(datasetPair);
        }
    }

    protected void reportDeviationsAndOmissionsForDatasetPair(ResourcePair datasetPair) {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByDistinctValueByDataset = resourcesByDistinctValueByDatasetByVariable.get(variable);
            if (theAspect.variableCoveredByDatasets(variable, datasetPair.first, datasetPair.second)) {
                Map<RDFNode, Set<Resource>> resourceByDistinctValuesOfFirstDataset = resourcesByDistinctValueByDataset.get(datasetPair.first);
                Map<RDFNode, Set<Resource>> resourceByDistinctValuesOfSecondDataset = resourcesByDistinctValueByDataset.get(datasetPair.second);
                for (Resource firstResource : correspondingResourcesByDataset.get(datasetPair.first)) {
                    for (Resource secondResource : correspondingResourcesByDataset.get(datasetPair.second)) {
                        Set<RDFNode> uncoveredValuesOfFirstResource =
                                getUncoveredValuesOfResource(firstResource, secondResource, resourceByDistinctValuesOfFirstDataset, resourceByDistinctValuesOfSecondDataset);
                        Set<RDFNode> uncoveredValuesOfSecondResource =
                                getUncoveredValuesOfResource(secondResource, firstResource, resourceByDistinctValuesOfSecondDataset, resourceByDistinctValuesOfFirstDataset);

                        // deviation: a pair of resources with each having a value not present in the
                        // other resource
                        // omission: a pair of resources with one having a value not present in the other,
                        // but not vice versa

                        // report missing not matching values
                        if (uncoveredValuesOfFirstResource.isEmpty()) {
                            for (RDFNode value2 : uncoveredValuesOfSecondResource) {
                                if (notKnownWrongValue(secondResource, variable, value2, datasetPair.second)) {
                                    Metadata.addValuesOmission(firstResource, variable, datasetPair.second, secondResource, value2, aspect,
                                            getOutputMetaModel(datasetPair.first));
                                }
                            }
                        } else if (uncoveredValuesOfSecondResource.isEmpty()) {
                            for (RDFNode value1 : uncoveredValuesOfFirstResource) {
                                if (notKnownWrongValue(firstResource, variable, value1, datasetPair.first)) {
                                    Metadata.addValuesOmission(secondResource, variable, datasetPair.first, firstResource, value1, aspect,
                                            getOutputMetaModel(datasetPair.second));
                                }
                            }
                        } else {
                            // report pairs of deviating values
                            for (RDFNode value1 : uncoveredValuesOfFirstResource) {
                                for (RDFNode value2 : uncoveredValuesOfSecondResource) {
                                    if (notKnownWrongValue(secondResource, variable, value2, datasetPair.second)) {
                                        Metadata.addDeviation(firstResource.asResource(), variable, value1, datasetPair.second,
                                                secondResource.asResource(), value2, aspect, getOutputMetaModel(datasetPair.first));
                                    }
                                    if (notKnownWrongValue(firstResource, variable, value1, datasetPair.first)) {
                                        Metadata.addDeviation(secondResource.asResource(), variable, value2, datasetPair.first,
                                                firstResource.asResource(), value1, aspect, getOutputMetaModel(datasetPair.second));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected Set<RDFNode> getUncoveredValuesOfResource(Resource resource, Resource comparedResource, Map<RDFNode, Set<Resource>> resourcesByValues, Map<RDFNode, Set<Resource>> comparedResourcesByValues) {
        Set<RDFNode> uncoveredValuesOfResource = new HashSet<>();
        Set<RDFNode> values = resourcesByValues.keySet();
        Set<RDFNode> comparedValues = comparedResourcesByValues.keySet();
        iterationOfComparedValues:
        for (RDFNode value : values) {
            if (isValueOfResource(value, resource, resourcesByValues)) {
                for (RDFNode comparedValue : comparedValues) {
                    if (isValueOfResource(comparedValue, comparedResource, comparedResourcesByValues)) {
                        if (equivalentValues(value, comparedValue)) {
                            continue iterationOfComparedValues;
                        }
                    }
                }
                uncoveredValuesOfResource.add(value);
            }
        }
        return uncoveredValuesOfResource;
    }

    boolean isValueOfResource(RDFNode value, Resource resource, Map<RDFNode, Set<Resource>> resourcesByValues) {
        return resourcesByValues.get(value).contains(resource);
    }

    protected boolean notKnownWrongValue(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
                                         Resource affectedDataset) {
        return !Metadata.isWrongValue(affectedResource, affectedVariableName, affectedValue, aspect,
                this.getInputMetaModelUnion(affectedDataset));
    }

    protected void compareValuesOfNotCorrespondingResources() {
        for (Resource dataset : datasets) {
            Set<Resource> uncoveredResources = unprocessedResourcesByDataset.get(dataset);
            for (Resource uncoveredResource : uncoveredResources) {
                Map<String, Set<RDFNode>> valuesByVariable = getValuesByVariable(dataset, uncoveredResource);
                for (String variable : valuesByVariable.keySet()) {
                    Set<RDFNode> valuesOfVariable = valuesByVariable.get(variable);
                    measureCountAndDeduplicatedCount(dataset, variable, valuesOfVariable);
                }
            }
        }
    }

    protected Map<String, Set<RDFNode>> getValuesByVariable(Resource dataset, Resource resource) {
        if (!theAspect.coversDataset(dataset)) {
            return Collections.emptyMap();
        }
        Query query = getQueryForResource(dataset, resource);
        Model model = getInputPrimaryModelUnion(dataset);
        return getValuesByVariable(model, query);
    }

    protected Query getQueryForResource(Resource dataset, Resource resource) {
        Query aspectPatternOfDataset = theAspect.getPattern(dataset).cloneQuery();
        Var keyVariable = theAspect.getKeyVariable();
        return SelectBuilder.rewrite(aspectPatternOfDataset, Collections.singletonMap(keyVariable, resource.asNode()));
    }

    protected Map<String, Set<RDFNode>> getValuesByVariable(Model model, Query query) {
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model)) {
            ResultSet results = queryExecution.execSelect();
            List<String> relevantVariables = getRelevantVariables(results);
            Map<String, Set<RDFNode>> valuesByVariable = new HashMap<>();
            for (String variable : relevantVariables) {
                valuesByVariable.put(variable, new HashSet<>());
            }
            while (results.hasNext()) {
                QuerySolution result = results.next();
                for (String variable : relevantVariables) {
                    if (result.contains(variable)) {
                        RDFNode value = result.get(variable);
                        if (!isExcludedValue(value)) {
                            valuesByVariable.get(variable).add(value);
                        }
                    }
                }
            }
            return valuesByVariable;
        }
    }

    List<String> getRelevantVariables(ResultSet results) {
        List<String> relevantVariables = results.getResultVars();
        relevantVariables.retainAll(variables);
        return relevantVariables;
    }

    /**
     * Checks if a value should be excluded from the comparison.
     *
     * @param value the value to check
     * @return {@code true}, if the value should not be used, otherwise {@code false}
     */
    protected boolean isExcludedValue(RDFNode value) {
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

    protected void measureCountAndDeduplicatedCount(Resource dataset, String variable, Set<RDFNode> valuesOfVariable) {
        long valuesCountWithDuplicates = valuesOfVariable.size();
        long valuesCountWithoutDuplicates = countDistinctValues(valuesOfVariable);
        nonDistinctValuesCount.get(variable).incrementByOrSet(dataset, valuesCountWithDuplicates);
        distinctValuesCount.get(variable).incrementByOrSet(dataset, valuesCountWithoutDuplicates);
    }

    protected int countDistinctValues(Set<RDFNode> nonDistinctValues) {
        List<RDFNode> distinctValues = new ArrayList<>(nonDistinctValues.size());
        iterationOfNonDistinctValues:
        for (RDFNode nonDistinctValue : nonDistinctValues) {
            for (RDFNode distinctValue : distinctValues) {
                if (equivalentValues(nonDistinctValue, distinctValue)) {
                    continue iterationOfNonDistinctValues;
                }
            }
            distinctValues.add(nonDistinctValue);
        }
        return distinctValues.size();
    }

    private void calculateDuplicateCount() {
        for (String variable : variables) {
            Count nonDistinctValuesCountOfVariable = nonDistinctValuesCount.get(variable);
            DeduplicatedCount distinctValuesCountOfVariable = distinctValuesCount.get(variable);
            DuplicateCount duplicateCountOfVariable = DuplicateCount.calculate(nonDistinctValuesCountOfVariable, distinctValuesCountOfVariable);
            duplicateCountOfVariable.setVariable(variable);
            duplicateValuesCount.put(variable, duplicateCountOfVariable);
        }
    }

    private void calculateRelativeCoveredness() {
        for (String variable : variables) {
            AbsoluteCoveredness absoluteValueCoverednessOfVariable = absoluteValueCoveredness.get(variable);
            DeduplicatedCount distinctValuesCountOfVariable = distinctValuesCount.get(variable);
            RelativeCoveredness relativeCoverednessOfVariable = RelativeCoveredness.calculate(absoluteValueCoverednessOfVariable, distinctValuesCountOfVariable);
            relativeCoverednessOfVariable.setVariable(variable);
            relativeValueCoveredness.put(variable, relativeCoverednessOfVariable);
        }
    }

    protected void calculateRelativeCoverage() {
        for (String variable : variables) {
            AbsoluteCoverage absoluteCoverageOfVariable = absoluteValueCoverage.get(variable);
            DeduplicatedCount distinctValuesCountOfVariable = distinctValuesCount.get(variable);
            RelativeCoverage relativeCoverageOfVariable = RelativeCoverage.calculate(absoluteCoverageOfVariable, distinctValuesCountOfVariable);
            relativeCoverageOfVariable.setVariable(variable);
            relativeValueCoverage.put(variable, relativeCoverageOfVariable);
        }
    }

    protected void calculateCompleteness() {
        for (String variable : variables) {
            AbsoluteCoverage absoluteCoverageOfVariable = absoluteValueCoverage.get(variable);
            DeduplicatedCount distinctValuesCountOfVariable = distinctValuesCount.get(variable);
            Completeness valueCompletenessOfVariable = Completeness.calculate(absoluteCoverageOfVariable, distinctValuesCountOfVariable);
            valueCompletenessOfVariable.setVariable(variable);
            valueCompleteness.put(variable, valueCompletenessOfVariable);
        }
    }

    protected void storeMeasures() {
        Measure.storeMeasuresByVariableInModel(nonDistinctValuesCount, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(distinctValuesCount, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(duplicateValuesCount, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(absoluteValueCoverage, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(relativeValueCoverage, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(absoluteValueCoveredness, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(relativeValueCoveredness, theAspect, outputMetaModelByDataset);
        Measure.storeMeasuresByVariableInModel(valueCompleteness, theAspect, outputMetaModelByDataset);
    }
}
