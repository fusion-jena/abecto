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
import de.uni_jena.cs.fusion.abecto.measure.*;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OM;
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
    Set<ResourceTupel> datasetTupels;
    Map<Resource, Model> outputMetaModelByDataset;
    /**
     * Number of covered values of another dataset, per variable.
     */
    Map<String, PerDatasetPairCount> absoluteValueCoverage;
    Map<String, PerDatasetTupelRatio> relativeValueCoverage;
    /**
     * Number of values in this dataset, per variable.
     */
    Map<String, PerDatasetCount> nonDistinctValuesCount;
    /**
     * Number of distinct values in this dataset, per variable. Index: variable, affectedDataset
     */
    Map<String, PerDatasetCount> distinctValuesCount;
    Map<String, PerDatasetRatio> valueCompleteness;

    Map<Resource, Set<Resource>> uncoveredResourcesByDataset = new HashMap<>();
    Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> resourcesByNonDistinctValueByDatasetByVariable = new HashMap<>();
    Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> resourcesByDistinctValueByDatasetByVariable = new HashMap<>();
    Map<Resource, Set<Resource>> correspondingResourcesByDataset = new HashMap<>();

    @Override
    public final void run() {
        setAspect(aspect);
        setAspectDatasets();
        initializeMeasures();
        loadRelevantResources();

        compareValuesOfCorrespondingResources();
        compareValuesOfNotCorrespondingResources();

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
        datasetTupels = ResourceTupel.getTupelsOf(datasets);
        outputMetaModelByDataset = getOutputMetaModels(datasets);
        initializeCorrespondingResourceByDataset();
        resourcesByNonDistinctValueByDatasetByVariable = getMapOfResourcesByValueByDatasetByVariable();
        resourcesByDistinctValueByDatasetByVariable = getMapOfResourcesByValueByDatasetByVariable();
    }

    protected void initializeCorrespondingResourceByDataset() {
        correspondingResourcesByDataset = new HashMap<>();
        for (Resource dataset : datasets) {
            correspondingResourcesByDataset.put(dataset, new HashSet<>());
        }
    }

    protected void initializeMeasures() {
        nonDistinctValuesCount = PerDatasetCount.mapByVariable(variables, AV.count, OM.one);
        setCoveredVariablesZero(nonDistinctValuesCount);
        distinctValuesCount = PerDatasetCount.mapByVariable(variables, AV.deduplicatedCount, OM.one);
        absoluteValueCoverage = PerDatasetPairCount.mapOfCountsInitializedToZero(variables, AV.absoluteCoverage, OM.one, datasetPairs); //TODO limit to variable covering datasets
        relativeValueCoverage = PerDatasetTupelRatio.mapOfRatios(variables, AV.relativeCoverage, OM.one);
        valueCompleteness = new HashMap<>();
    }

    protected void setCoveredVariablesZero(Map<String, PerDatasetCount> measures) {
        for (String variable : variables) {
            for (Resource dataset : datasets) {
                if (theAspect.variableCoveredByDataset(variable, dataset)) {
                    measures.get(variable).setZero(dataset);
                }
            }
        }
    }

    protected void loadRelevantResources() {
        uncoveredResourcesByDataset.clear();
        for (Resource dataset : datasets) {
            setResourcesOfDatasetAndAspectUncovered(dataset);
        }
    }

    protected void compareValuesOfCorrespondingResources() {
        getCorrespondenceGroups().forEach(this::compareValuesOfCorrespondingResources);
    }

    protected void compareValuesOfCorrespondingResources(List<Resource> correspondingResources) {
        setCorrespondingResourcesByDataset(correspondingResources);
        removeCorrespondingResourcesFromUncoveredResources();

        loadNonDistinctValues();
        calculateDistinctValues();

        measureNonDistinctValuesCount();
        measureDistinctValuesCount();
        measureAbsoluteCoverage();
        reportDeviationsAndOmissions();
    }

    protected void setCorrespondingResourcesByDataset(List<Resource> correspondingResources) {
        for (Resource dataset : datasets) {
            Set<Resource> correspondingResourcesOfDataset = correspondingResourcesByDataset.get(dataset);
            correspondingResourcesOfDataset.clear();
            correspondingResourcesOfDataset.addAll(correspondingResources);
            correspondingResourcesOfDataset.retainAll(uncoveredResourcesByDataset.get(dataset));
        }
    }

    protected void loadNonDistinctValues() {
        resetResourcesByNonDistinctValueByDatasetByVariable();

        Set<Resource> datasetsOfCorrespondingResources = correspondingResourcesByDataset.keySet();

        for (Resource dataset : datasetsOfCorrespondingResources) {
            Collection<Resource> resourcesOfDataset = correspondingResourcesByDataset.get(dataset);
            loadNonDistinctValuesOfDataset(dataset, resourcesOfDataset);
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

    protected Query getQueryForResource(Resource dataset, Resource resource) {
        Query aspectPatternOfDataset = theAspect.getPattern(dataset).cloneQuery();
        Var keyVariable = theAspect.getKeyVariable();
        return SelectBuilder.rewrite(aspectPatternOfDataset, Collections.singletonMap(keyVariable, resource.asNode()));
    }

    protected void resetResourcesByNonDistinctValueByDatasetByVariable() {
        for (Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByNonDistinctValueByDataset : resourcesByNonDistinctValueByDatasetByVariable.values()) {
            for (Map<RDFNode, Set<Resource>> resourcesByNonDistinctValue : resourcesByNonDistinctValueByDataset.values()) {
                resourcesByNonDistinctValue.clear();
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

    protected void measureNonDistinctValuesCount() {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByNonDistinctValueByDataset = resourcesByNonDistinctValueByDatasetByVariable.get(variable);
            PerDatasetCount nonDistinctValuesCountOfVariable = nonDistinctValuesCount.get(variable);
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
                    distinctValuesCount.get(variable).incrementByOrSet(dataset, resourcesByDistinctValue.size());
                }
            }
        }
    }

    protected void measureAbsoluteCoverage() {
        for (String variable : variables) {
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByDistinctValueByDataset = resourcesByDistinctValueByDatasetByVariable.get(variable);
            PerDatasetPairCount absoluteCoverageForVariable = absoluteValueCoverage.get(variable);
            for (ResourcePair datasetPair : datasetPairs) {
                if (theAspect.variableCoveredByDatasets(variable, datasetPair.first, datasetPair.second)) {
                    Set<RDFNode> distinctValuesOfFirstDataset = resourcesByDistinctValueByDataset.get(datasetPair.first).keySet();
                    Set<RDFNode> distinctValuesOfSecondDataset = resourcesByDistinctValueByDataset.get(datasetPair.second).keySet();
                    for (RDFNode valueOfFirstDataset : distinctValuesOfFirstDataset) {
                        for (RDFNode valueOfSecondDataset : distinctValuesOfSecondDataset) {
                            if (equivalentValues(valueOfFirstDataset, valueOfSecondDataset)) {
                                absoluteCoverageForVariable.incrementByOrSetOne(datasetPair);
                                break;
                            }
                        }
                    }
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
            Map<Resource, Map<RDFNode, Set<Resource>>> resourcesByNonDistinctValueByDataset = resourcesByNonDistinctValueByDatasetByVariable.get(variable);
            if (theAspect.variableCoveredByDatasets(variable, datasetPair.first, datasetPair.second)) {
                Map<RDFNode, Set<Resource>> resourceByNonDistinctValuesOfFirstDataset = resourcesByNonDistinctValueByDataset.get(datasetPair.first);
                Map<RDFNode, Set<Resource>> resourceByNonDistinctValuesOfSecondDataset = resourcesByNonDistinctValueByDataset.get(datasetPair.second);
                for (Resource firstResource : correspondingResourcesByDataset.get(datasetPair.first)) {
                    for (Resource secondResource : correspondingResourcesByDataset.get(datasetPair.second)) {
                        Set<RDFNode> uncoveredValuesOfFirstResource =
                                getUncoveredValuesOfResource(firstResource, secondResource, resourceByNonDistinctValuesOfFirstDataset, resourceByNonDistinctValuesOfSecondDataset);
                        Set<RDFNode> uncoveredValuesOfSecondResource =
                                getUncoveredValuesOfResource(secondResource, firstResource, resourceByNonDistinctValuesOfSecondDataset, resourceByNonDistinctValuesOfFirstDataset);

                        // deviation: a pair of resources with each having a value not present in the
                        // other resource
                        // omission: a pair of resources with one having a value not present in the other,
                        // but not vice versa

                        // report missing not matching values
                        if (uncoveredValuesOfFirstResource.isEmpty()) {
                            for (RDFNode value2 : uncoveredValuesOfSecondResource) {
                                if (!isKnownWrongValue(secondResource, variable, value2, datasetPair.second)) {
                                    Metadata.addValuesOmission(firstResource, variable, datasetPair.second, secondResource, value2, aspect,
                                            getOutputMetaModel(datasetPair.first));
                                }
                            }
                        } else if (uncoveredValuesOfSecondResource.isEmpty()) {
                            for (RDFNode value1 : uncoveredValuesOfFirstResource) {
                                if (!isKnownWrongValue(firstResource, variable, value1, datasetPair.first)) {
                                    Metadata.addValuesOmission(secondResource, variable, datasetPair.first, firstResource, value1, aspect,
                                            getOutputMetaModel(datasetPair.second));
                                }
                            }
                        } else {
                            // report pairs of deviating values
                            for (RDFNode value1 : uncoveredValuesOfFirstResource) {
                                for (RDFNode value2 : uncoveredValuesOfSecondResource) {
                                    if (!isKnownWrongValue(secondResource, variable, value2, datasetPair.second)) {
                                        Metadata.addDeviation(firstResource.asResource(), variable, value1, datasetPair.second,
                                                secondResource.asResource(), value2, aspect, getOutputMetaModel(datasetPair.first));
                                    }
                                    if (!isKnownWrongValue(firstResource, variable, value1, datasetPair.first)) {
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

    protected Map<String, Map<Resource, Map<RDFNode, Set<Resource>>>> getMapOfResourcesByValueByDatasetByVariable() {
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

    protected void removeCorrespondingResourcesFromUncoveredResources() {
        for (Resource dataset : correspondingResourcesByDataset.keySet()) {
            Set<Resource> uncoveredResourcesOfDataset = uncoveredResourcesByDataset.get(dataset);
            Set<Resource> coveredResourcesOfDataset = correspondingResourcesByDataset.get(dataset);
            uncoveredResourcesOfDataset.removeAll(coveredResourcesOfDataset);
        }
    }

    protected boolean isKnownWrongValue(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
                                        Resource affectedDataset) {
        return Metadata.isWrongValue(affectedResource, affectedVariableName, affectedValue, aspect,
                this.getInputMetaModelUnion(affectedDataset));
    }

    protected void setResourcesOfDatasetAndAspectUncovered(Resource dataset) {
        Set<Resource> distinctResources = getResourceKeys(theAspect, dataset).collect(Collectors.toSet());
        uncoveredResourcesByDataset.put(dataset, distinctResources);
    }

    protected void compareValuesOfNotCorrespondingResources() {
        for (Resource dataset : datasets) {
            Set<Resource> uncoveredResources = uncoveredResourcesByDataset.get(dataset);
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

    protected void calculateRelativeCoverage() {
        for (String variable : variables) {
            PerDatasetTupelRatio relativeCoverageOfVariable = relativeValueCoverage.get(variable);
            PerDatasetPairCount absoluteCoverageOfVariable = absoluteValueCoverage.get(variable);
            PerDatasetCount deduplicatedCountOfVariable = distinctValuesCount.get(variable);
            relativeCoverageOfVariable.setRatioOf(absoluteCoverageOfVariable, deduplicatedCountOfVariable);
        }
    }

    protected void calculateCompleteness() {
        for (String variable : variables) {
            PerDatasetRatio valueCompletenessOfVariable = calculateCompleteness(datasetPairs, absoluteValueCoverage.get(variable), distinctValuesCount.get(variable));
            valueCompleteness.put(variable, valueCompletenessOfVariable);
        }
    }

    protected void storeMeasures() {
        // TODO add value exclusion filter description to measurement description
        Measure.storeInModelForAllVariable(nonDistinctValuesCount, theAspect, outputMetaModelByDataset);
        // TODO add value exclusion filter description to measurement description
        Measure.storeInModelForAllVariable(distinctValuesCount, theAspect, outputMetaModelByDataset);
        // TODO add value exclusion filter description to measurement description
        Measure.storeInModelForAllVariable(absoluteValueCoverage, theAspect, outputMetaModelByDataset);
        // TODO add value exclusion filter description to measurement description
        Measure.storeInModelForAllVariable(relativeValueCoverage, theAspect, outputMetaModelByDataset);
        // TODO add value exclusion filter description to measurement description
        PerDatasetRatio.storeInModelAsComparedToAllOtherResourcesForAllVariables(valueCompleteness, theAspect, outputMetaModelByDataset);
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
}
