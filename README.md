[![Tests](https://github.com/fusion-jena/abecto/actions/workflows/maven.yml/badge.svg)](https://github.com/fusion-jena/abecto/actions/workflows/maven.yml)
[![DOI](https://zenodo.org/badge/261377020.svg)](https://zenodo.org/badge/latestdoi/261377020)

# ABECTO

ABECTO is an ABox Evaluation and Comparison Tool for Ontologies.

<!-- TOC -->
* [ABECTO](#abecto)
  * [Project Examples](#project-examples)
  * [License](#license)
  * [Publications](#publications)
* [How to Use ABECTO?](#how-to-use-abecto)
  * [Building](#building)
  * [Configuration](#configuration)
    * [How to write an ABECTO plan?](#how-to-write-an-abecto-plan)
  * [Execution](#execution)
  * [CI Usage with Docker](#ci-usage-with-docker)
* [ABECTO Processors](#abecto-processors)
  * [Source Processors](#source-processors)
  * [Transformation Processors](#transformation-processors)
  * [Mapping Processors](#mapping-processors)
  * [Comparison Processors](#comparison-processors)
* [ABECTO Reports](#abecto-reports)
<!-- TOC -->

## Project Examples
* [Comparison and Evaluation of Unit Ontologies with ABECTO](https://github.com/fusion-jena/abecto-unit-ontology-comparison)
* [Comparison of Space Travel Data in Wikidata and DBpedia with ABECTO](https://github.com/fusion-jena/abecto-space-travel-comparison)

## License

ABECTO is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Publications

In case you use this implementation for your scientific work, please consider to cite the related paper:

* Keil, Jan Martin (2020). **[ABECTO: An ABox Evaluation and Comparison Tool for Ontologies](https://preprints.2020.eswc-conferences.org/posters_demos/paper_298.pdf)**. In: ESWC 2020 Satellite Events: Posters and Demos. [DOI:10.1007/978-3-030-62327-2_24](https://doi.org/10.1007/978-3-030-62327-2_24).

Further related publications:

* Keil, Jan Martin (2018). **[Ontology ABox Comparison](https://fusion.cs.uni-jena.de/fusion/publications/ontology-abox-comparison/)**. In: ESWC 2018 Satellite Events: PhD Symposium. [DOI:10.1007/978-3-319-98192-5_43](https://doi.org/10.1007/978-3-319-98192-5_43).

# How to Use ABECTO?

This sections provides an overview about the use of ABECTO.

## Building

To use ABECTO, first checkout the project and compile ABECTO using Maven:

```shell
mvn -B -Dmaven.test.skip=true package
```

This will create a stand alone .jar file at [target/abecto.jar](target).

## Configuration

The execution of ABECTO is configured in a plan file, which is an RDF dataset file ([TriG](https://www.w3.org/TR/trig/), [N-Quads](https://www.w3.org/TR/n-quads/), …), using the [ABECTO Vocabulary](http://w3id.org/abecto/vocabulary). For an example see the [tutorial configuration](src/test/resources/tutorial-configuration.trig). Further build in processors can be found in the section [ABECTO Processors](#abecto-processors).

### How to write an ABECTO plan?

1. **Namespace:** To ease writing add a base declaration for the IRIs of your plan and some prefix declarations. A good start is the following example. You might adapt the base IRI and add further prefixes related to the compared knowledge graphs.

   ```turtle
   @base                     <http://example.org/> .
   @prefix abecto:           <java:de.uni_jena.cs.fusion.abecto.processor.> .
   @prefix av:               <http://w3id.org/abecto/vocabulary#> .
   @prefix pplan:            <http://purl.org/net/p-plan#> .
   @prefix rdf:              <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
   @prefix rdfs:             <http://www.w3.org/2000/01/rdf-schema#> .
   @prefix xsd:              <http://www.w3.org/2001/XMLSchema#> .
   ```

2. **Plan:** Declare the plan

   ```turtle
   <plan> a av:Plan ;
       .
   ```

3. **Aspects:** Specify the resources to compare by adding at least one aspect declaration. The following example declares the aspect `<aspectPerson>` with the key variable `person` covered by three datasets (`<dataset1>`, `<dataset2>`, `<dataset3>`). For each dataset a defining SPARQL select query is declared that returns the key variable and further variables to compare.

   ```turtle
   <aspectPerson> a av:Aspect ;
       av:keyVariableName "person" ;
       .
   []  a av:AspectPattern ;
       av:ofAspect <aspectPerson> ;
       av:associatedDataset <dataset1> ;
       av:definingQuery """
           SELECT ?person ?label ?pnr ?boss
           WHERE {
               ?person <http://www.w3.org/2000/01/rdf-schema#label> ?label ;
                   <http://example.org/a/pnr> ?pnr ;
                   <http://example.org/a/boss> ?boss .
           }
       """ ;
       .
   []  a av:AspectPattern ;
       av:ofAspect <aspectPerson> ;
       av:associatedDataset <dataset2> ;
       av:definingQuery """
           SELECT ?person ?label ?boss
           WHERE {
               ?person <http://www.w3.org/2000/01/rdf-schema#label> ?label .
               OPTIONAL { ?person <http://example.org/b/boss> ?boss }
           }
       """ ;
       .
   []  a av:AspectPattern ;
       av:ofAspect <aspectPerson> ;
       av:associatedDataset <dataset3> ;
       av:definingQuery """
           SELECT ?person ?label ?pnr
           WHERE {
               ?person <http://www.w3.org/2000/01/rdf-schema#label> ?label ;
                   <http://example.org/c/pnr> ?pnr .
           }
       """ ;
       .
   ```

4. **Source Steps:** Specify plan steps that load the sources of the primary data of the datasets. The following example declares steps to load primary data of three dataset from four files. Each source step is an independent starting point of the plan without preceding steps. Providing `rdfs:label` to the steps will ease reading the execution logs and reports.

   ```turtle
   <source1> a av:Step ;
       rdfs:label "Load Dataset 1"@en;
       p-plan:isStepOfPlan <plan> ;
       av:processorClass abecto:FileSourceProcessor ;
       av:hasParameter [av:key "path" ; av:value "tutorial-source1part1.ttl", "tutorial-source1part2.ttl" ] ;
       av:associatedDataset <dataset1> ;
       .

   <source2> a av:Step ;
       rdfs:label "Load Dataset 2"@en;
       p-plan:isStepOfPlan <plan> ;
       av:processorClass abecto:FileSourceProcessor ;
       av:hasParameter [av:key "path" ; av:value "tutorial-source2.ttl" ] ;
       av:associatedDataset <dataset2> ;
       .

   <source3> a av:Step ;
       rdfs:label "Load Dataset 3"@en;
       p-plan:isStepOfPlan <plan> ;
       av:processorClass abecto:FileSourceProcessor ;
       av:hasParameter [av:key "path" ; av:value "tutorial-source3.ttl" ] ;
       av:associatedDataset <dataset3> ;
       .
   ```

5. **Transformation, Mapping, and Comparison/Evaluation Steps:** Specify further steps for transforming, mapping and comparing the primary data. Each step will have at least one preceding steps specified using `p-plan:isPrecededBy`. With `av:predefinedMetaDataGraph` steps you can manually add mappings, mapping exclusions or annotations into the process. The following example declares a mapping step `<jaroWinklerMapping>` to map resources based on string similarity and a comparison step `<literalValueComparison>` to compare literal values. At the mapping step, a predefined metadata graph `<manualMappings>` is introduced to prevent the mapping of two resources. The mapping step is preceded by all three source steps. The comparison step is only directly preceded by the mapping step. But the data returned by all indirect preceding steps are also available. 

   ```turtle
   GRAPH <manualMappings>
   {
       <http://example.org/b/william> av:correspondsNotToResource <http://example.org/c/P004> .
       <aspectPerson> av:relevantResource <http://example.org/b/william>, <http://example.org/c/P004> .
   }

   <jaroWinklerMapping> a av:Step ;
       rdfs:label "Person Mapping using Name Similarity"@en;
       p-plan:isStepOfPlan <plan> ;
       av:processorClass abecto:JaroWinklerMappingProcessor ;
       p-plan:isPrecededBy <source1>, <source2>, <source3> ;
       av:predefinedMetaDataGraph <manualMappings> ;
       av:hasParameter
           [av:key "threshold" ; av:value 9e-1 ] ,
           [av:key "caseSensitive" ; av:value false ] ,
           [av:key "aspect" ; av:value <aspectPerson> ] ,
           [av:key "variables" ; av:value "label" ] ;
       .
    .
   <propertyComparison> a av:Step ;
       rdfs:label "Comparison of Persons Boss, Name and PNR"@en;
       p-plan:isStepOfPlan <plan> ;
       av:processorClass abecto:PropertyComparisonProcessor ;
       p-plan:isPrecededBy <jaroWinklerMapping> ;
       av:hasParameter
           [av:key "aspect" ; av:value <aspectPerson> ] ,
           [av:key "variables" ; av:value "boss", "label", "pnr" ] ;
       .
   ```

## Execution

ABECTO has the following options and parameters:

```
Usage: abecto [-hV] [--failOnDeviation] [--failOnIssue]
              [--failOnResourceOmission] [--failOnValueOmission]
              [--failOnWrongValue] [--loadOnly] [-p=IRI] [--reportOn=IRI]
              [--trig=FILE] [-E=TEMPLATE_NAME=FILE]... FILE
Compares and evaluates several RDF datasets.
      FILE                 RDF dataset file containing the plan configuration
                             and optionally plan execution results (see
                             --loadOnly).
  -E, --export=TEMPLATE_NAME=FILE
                           Template and output file for an result export. Can
                             be set multiple times.
      --failOnDeviation    If set, a exit code > 0 will be returned, if the
                             results contain a deviation. Useful together with
                             "--reportOn".
      --failOnIssue        If set, a exit code > 0 will be returned, if the
                             results contain an issue. Useful together with
                             "--reportOn".
      --failOnResourceOmission
                           If set, a exit code > 0 will be returned, if the
                             results contain a resource omission. Useful
                             together with "--reportOn".
      --failOnValueOmission
                           If set, a exit code > 0 will be returned, if the
                             results contain a value omission. Useful together
                             with "--reportOn".
      --failOnWrongValue   If set, a exit code > 0 will be returned, if the
                             results contain a wrong value. Useful together
                             with "--reportOn".
  -h, --help               Show this help message and exit.
      --loadOnly           If set, the plan will not get executed. This enables
                             to export results without repeated plan execution.
  -p, --plan=IRI           IRI of the plan to process. Required, if the
                             configuration contains multiple plans.
      --reportOn=IRI       IRI of the dataset to report on. Reports will get
                             limited to results about this dataset.
      --trig=FILE          RDF TRIG dataset file for the execution results.
  -V, --version            Print version information and exit.
```

Examples:
* show the help message:
  ```shell
  java -jar target/abecto.jar --help
  ```
* run the tutorial plan and store the result:
  ```shell
  java -jar target/abecto.jar --trig result.trig src/test/resources/tutorial-configuration.trig
  ```
* create a deviations report for a specific dataset and without re-running the plan:
  ```shell
  java -jar target/abecto.jar --loadOnly --reportOn "http://example.org/dataset1" --export deviations=deviations.csv result.trig
  ```

## CI Usage with Docker

Docker images for ABECTO are provide on GitHub Packages.
Inside the Docker images the ABECTO .jar file is located at `/opt/abecto.jar`, but an alias is configured allowing to type `abecto` instead of `java -jar /opt/abecto.jar`.

Example configuration for use in CI pipeline on GitLab:
```yaml
image: ghcr.io/fusion-jena/abecto:latest

abecto:
  stage: test
  script:
    - abecto --trig result.trig tutorial-configuration.trig
  artifacts:
    paths:
      - result.trig
```

Example configuration for use in CI pipeline on GitHub:
```yaml
name: ABECTO CI
on: [push, pull_request, workflow_dispatch]

jobs:
  compare:
    runs-on: ubuntu-latest
    container: ghcr.io/fusion-jena/abecto:latest
    steps:
      - name: Checkout Project
        uses: actions/checkout@v2
      - name: Run ABECTO
        run: abecto --trig result.trig tutorial-configuration.trig
      - name: Archive Comparison Result
        uses: actions/upload-artifact@v2
        with:
          name: Comparison Result RDF Dataset
          path: result.trig
```

# ABECTO Processors

ABECTO has a couple of built-in processors.
In the pipeline description, processors get denoted by an IRI of the unofficial scheme `java` and a path equal to the canonical name of the processors class.
This way to represent Java classes is used also in Apache Jena.
We use the following prefix to abbreviate the namespace in the processor IRIs:

```
@prefix abecto: <java:de.uni_jena.cs.fusion.abecto.processor.> .
```

## Source Processors

Source Processors load RDF data from different sources and store them in the internal triple store for further processing.

The **File Source Processor** ([abecto:FileSourceProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/FileSourceProcessor.java)) loads RDF data from one or multiple locale files of one of the following formats: RDF/XML, TriG, N-Quads, Turtle, N-Triples, JSON-LD, SHACL Compact Syntax, TriX, and RDF Thrift.
The format is automatically detected.
The processor has the following parameter:

| name | description                                             | default |
|------|---------------------------------------------------------|---------|
| path | One or multiple paths of RDF files that will be loaded. |         |
A path may either be absolute, or relative to the configuration file.

The **SPARQL Source Processor** ([abecto:SparqlSourceProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/SparqlSourceProcessor.java)) loads RDF data from a SPARQL endpoint.
This makes ABECTO independent of the availability of knowledge graphs RDF dumps and may avoid the handling of large dump files, if only a small share of the data is needed.
The resources of interest get defined by a SPARQL query, a list, or both.
The processor partitions the requested resources into chunks and loads all statements containing resources of the chunk as subject or object.
Depending on the given parameters, the other non-predicate resources of loaded statements will also get loaded until a certain distance.
Further parameters enable fine-grained control of statements to load and for the handling of errors like endpoint time-outs.
The processor has the following parameters:

| name                    | description                                                                                                                                                                                                                                                                                        | default                              |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|
| service                 | URL of the SPARQL endpoint to use.                                                                                                                                                                                                                                                                 |                                      |
| list                    | List of the relevant resources.                                                                                                                                                                                                                                                                    |                                      |
| query                   | SELECT query to retrieve a list of the relevant resources. All variables will be taken into account. None IRI values will be ignored. ORDER BY, LIMIT and OFFSET might become overwritten.                                                                                                         |                                      |
| chunkSize               | Maximum number of resources to retrieve in one request.                                                                                                                                                                                                                                            | 500                                  |
| chunkSizeDecreaseFactor | Factor to reduce the `chunkSize` after failed request to the source SPARQL endpoint.                                                                                                                                                                                                               | 0.5                                  |
| chunkSizeIncreaseFactor | Factor to increase the `chunkSize` after successful request to the source SPARQL endpoint until the initial value got restored.                                                                                                                                                                    | 1.5                                  |
| maxDistance             | Maximum distance of loaded associated resources. Associated resources share a statement as an object with a retrieved resource as a subject, in case of any property, and vice versa, in case of followed inverse properties (see `followInverse`).                                                | 0                                    |
| followInverse           | Properties to track in inverse direction to compile a list of associated resources to load. That means that the subject of a statement whose property is in this list and whose object is a loaded resource will become an associated resource.                                                    |                                      |
| followUnlimited         | Properties that represent a hierarchy. Resources associated to a loaded resource by a followUnlimited property will be loaded unlimited, but will not cause retrieval of further resources not connected by a followUnlimited property or a followInverseUnlimited property.                       | rdfs:subClassOf, rdf:first, rdf:rest |
| followInverseUnlimited  | Properties that represent a hierarchy. Resources associated to a loaded resource by the inverse of a followInverseUnlimited property will be loaded unlimited, but will not cause retrieval of further resources not connected by a followUnlimited property or a followInverseUnlimited property. |                                      |
| ignoreInverse           | Properties to ignore in inverse direction. Statements with one of these properties will neither get loaded nor will their subjects become an associated resource.                                                                                                                                  |                                      |
| maxRetries              | Total maximum number of retries of failed request to the source SPARQL endpoint.                                                                                                                                                                                                                   | 128                                  |

The **URL Source Processor** ([abecto:UrlSourceProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/UrlSourceProcessor.java)) loads RDF data from one or multiple remote files of one of the following formats: RDF/XML, TriG, N-Quads, Turtle, N-Triples, JSON-LD, SHACL Compact Syntax, TriX, and RDF Thrift.
The format is automatically detected.
The processor has the following parameter:

| name | description                                            | default |
|------|--------------------------------------------------------|---------|
| url  | One or multiple URLs of RDF files that will be loaded. |         |

## Transformation Processors

Transformation processors derive additional primary data from the existing primary data.
For example, this enables the derivation of implicit statements or the adjustment of value formatting for the mapping or comparison.

The **Forward Rule Reasoning Processor** ([abecto:ForwardRuleReasoningProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/ForwardRuleReasoningProcessor.java)) applies forward rules to derive additional primary data.
The processor has the following parameter:

| name  | description                                                                                                                              | default |
|-------|------------------------------------------------------------------------------------------------------------------------------------------|---------|
| rules | The rules to apply on the primary data using the [Apache Jena rule syntax](https://jena.apache.org/documentation/inference/#RULEsyntax). |         |

The **SPARQL Construct Processor** ([abecto:SparqlConstructProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/SparqlConstructProcessor.java)) applies a SPARQL construct query on the primary data of a knowledge graph to derive additional primary data.
The query execution will be repeated until a configured limit of execution or no new statements have been produced.
The processor has the following parameters:

| name          | description                                              | default |
|---------------|----------------------------------------------------------|---------|
| query         | The SPARQL construct query to apply on the primary data. |         |
| maxIterations | Maximum number of executions of the query.               | 1       |

## Mapping Processors

Mapping Processors provide correspondences and correspondence exclusions between resources in the knowledge graphs.
The pipeline of a comparison plan may contain multiple complementary mapping processors.
In case of contradicting results of mapping processors, the processor executed first takes precedence and contradicting correspondences or correspondence exclusions will not be added.
That way, it is also possible to provide manual adjustments to the mapping by providing correspondences or correspondence exclusions in a predefined metadata graph in the configuration.
A rule reasoner is used to derive implicit correspondences and correspondence exclusions.
The reasoning applies immediately on new correspondences to consider them during the further mapping processor execution.
Additionally, the inferences get persisted after a mapping processor execution succeeded.

The **Equivalent Value Mapping Processor** ([abecto:EquivalentValueMappingProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/EquivalentValueMappingProcessor.java)) provides correspondences between resources of one aspect in different knowledge graphs, if they have equivalent values for all given variables.
This is similar to the inferences of an OWL reasoner on inverse functional properties.
Values are treated as equivalent if they are equivalent literals or if they are resources that are already known to correspond.
If multiple values exist for one variable, only one pair of values must be equivalent.
Unbound variables are treated as not equivalent.
The processor has the following parameters:

| name      | description                                                                                   | default |
|-----------|-----------------------------------------------------------------------------------------------|---------|
| aspect    | The aspects for which the correspondences get generated.                                      |         |
| variables | One or multiple variables that will be compared to determine the correspondence of resources. |         |

The **Functional Mapping Processor** ([abecto:FunctionalMappingProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/FunctionalMappingProcessor.java)) provides correspondences based on links from resources of another aspect.
If corresponding resources from different knowledge graphs link with a given variable two resources, these resources will be considered to correspond.
This is similar to the inferences of an OWL reasoner on functional properties.
The processor has the following parameters:

| name              | description                                               | default |
|-------------------|-----------------------------------------------------------|---------|
| referringAspect   | The aspect of the resources linking the resources to map. |         |
| referringVariable | The variable linking the resources to map.                |         |
| referredAspect    | The aspect of the resources to map.                       |         |

The **Jaro-Winkler Mapping Processor** ([abecto:JaroWinklerMappingProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/JaroWinklerMappingProcessor.java)) provides correspondences based on the Jaro-Winkler Similarity (see [String Comparator Metrics and Enhanced Decision Rules in the Fellegi-Sunter Model of Record Linkage](http://eric.ed.gov/?id=ED325505)) of string values using our implementation for efficient bounded Jaro-Winkler similarity based search (see [Efficient Bounded Jaro-Winkler Similarity Based Search](http://doi.org/10.18420/btw2019-13)).
Two resources are considered to correspond if for one variable in both directions the other variable value is the most similar value from the other knowledge graph and if the similarity score exceeds a threshold.
The processor has the following parameters:

| name          | description                                                                              | default |
|---------------|------------------------------------------------------------------------------------------|---------|
| aspect        | The aspects for which the correspondences are generated.                                 |         |
| variables     | One or multiple variables used to search corresponding resources.                        |         |
| threshold     | The similarity threshold the variable values of two resources must comply.               |         |
| caseSensitive | Determines, if case is taken into account during the search for corresponding resources. |         |

The **Use Present Mapping Processor** ([abecto:UsePresentMappingProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/UsePresentMappingProcessor.java)) provides correspondences based on existing links between resources in variable values.
The processor has the following parameters:

| name     | description                                                     | default |
|----------|-----------------------------------------------------------------|---------|
| aspect   | The aspects for which the correspondences get generated.        |         |
| variable | The variable that links a resource to a corresponding resource. |         |

## Comparison Processors

Comparison processors compare the primary data of the knowledge graphs using the correspondences provided by the mapping processors.
They provide annotations on specific values, resources, and knowledge graphs or determine measurements on the knowledge graphs.

The **Population Comparison Processor** ([abecto:PopulationComparisonProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/PopulationComparisonProcessor.java)) provides on the one hand [av:Issue](http://w3id.org/abecto/vocabulary#Issue) annotations for resource duplicates and [av:ResourceOmission](http://w3id.org/abecto/vocabulary#ResourceOmission) annotations.
On the other hand, it provides per knowledge graph measurements of

* the count ([av:count](http://w3id.org/abecto/vocabulary#count)) of resources of an aspect,
* the duplicate-free count ([av:deduplicatedCount](http://w3id.org/abecto/vocabulary#deduplicatedCount)) of resources of an aspect,
* the absolute coverage ([av:absoluteCoverage](http://w3id.org/abecto/vocabulary#absoluteCoverage)) of resources of an aspect in another knowledge graph,
* the relative coverage ([av:relativeCoverage](http://w3id.org/abecto/vocabulary#relativeCoverage)) of resources of an aspect in another knowledge graph, and
* the estimated completeness ([av:marCompletenessThomas08](http://w3id.org/abecto/vocabulary#marCompletenessThomas08)) of resources of an aspect.

The estimated completeness is determined by a mark and recapture method as proposed by Razniewski et al. (see [But What Do We Actually Know?](http://doi.org/10.18653/v1/W16-1308)) and using the mark and recapture method defined by Thomas (see [Generalising multiple capture-recapture to non-uniform sample sizes](http://doi.org/10.1145/1390334.1390531)), which permits multiple samples of different sample sizes.
The processor has the following parameter:

| name    | description                                                                       | default |
|---------|-----------------------------------------------------------------------------------|---------|
| aspects | One or multiple aspects for which measurements and annotations will be generated. |         |

The **Property Comparison Processor** ([abecto:PropertyComparisonProcessor](src/main/java/de/uni_jena/cs/fusion/abecto/processor/PropertyComparisonProcessor.java)) provides [av:Deviation](http://w3id.org/abecto/vocabulary#Deviation), [av:ValuesOmission](http://w3id.org/abecto/vocabulary#ValuesOmission), and [av:Issue](http://w3id.org/abecto/vocabulary#Issue) annotations on property values for one variable of corresponding resources.
On the other hand, it provides per knowledge graph measurements of

* the count ([av:count](http://w3id.org/abecto/vocabulary#count)) of resources of an aspect,
* the duplicate-free count ([av:deduplicatedCount](http://w3id.org/abecto/vocabulary#deduplicatedCount)) of resources of an aspect,
* the absolute coverage ([av:absoluteCoverage](http://w3id.org/abecto/vocabulary#absoluteCoverage)) of resources of an aspect in another knowledge graph,
* the relative coverage ([av:relativeCoverage](http://w3id.org/abecto/vocabulary#relativeCoverage)) of resources of an aspect in another knowledge graph, and
* the estimated completeness ([av:marCompletenessThomas08](http://w3id.org/abecto/vocabulary#marCompletenessThomas08)) of resources of an aspect.

The estimated completeness is determined by a mark and recapture method as proposed by Razniewski et al. (see [But What Do We Actually Know?](http://doi.org/10.18653/v1/W16-1308)) and using the mark and recapture method defined by Thomas (see [Generalising multiple capture-recapture to non-uniform sample sizes](http://doi.org/10.1145/1390334.1390531)), which permits multiple samples of different sample sizes.

Object propert values are considered equivalent, if they correspond to each other up to the mapping.
Data property values are considered equivalent, if they are semantically equivalent with the following exceptions:
(a) Numeric literals are additionally considered as equivalent even if they have incompatible datatypes, i.e. pairs out of the three groups `xsd:float`, `xsd:double` and (datatypes derived from) `xsd:decimal`, if both literals either represent the same special value (`INF`, `-INF`, `NaN`) or represent equal numbers in the value space, but not necessarily if they have equal lexical representations (see [The Problem with XSD Binary Floating Point Datatypes in RDF](http://doi.org/10.1007/978-3-031-06981-9_10)).
For example, `"0.5"^^xsd:decimal` and `"0.5"^^xsd:float` are additionally considered as equivalent.
However, `"0.1"^^xsd:decimal` and `"0.1"^^xsd:float`, which actually represents the number 0.1000000014…, are not considered as equivalent.
(b) Depending on the parameters, temporal values of the types `xsd:date` and `xsd:dateTime` might get considered equivalent, if they have equivalent date parts.
(c) Depending on the parameters, string values of the type `xsd:string` or `rdf:langString` might get considered equivalent, even if they have equal lexical representations but different language tags.
The processor has the following parameters:

| name                   | description                                                                                                                                                                                                                                                                                                       | default              |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| aspect                 | The aspects for which annotations will be generated.                                                                                                                                                                                                                                                              |                      |
| variables              | One or multiple variables that will be compared to determine the annotations.                                                                                                                                                                                                                                     |                      |
| languageFilterPatterns | Zero, one or multiple language patterns to filter compared literals. Literals of datatype `xsd:string` and `rdf:langString` will be considered only, if they match at least on of these patterns. String literals without language tag will match with "", all string literals with  language tag match with "*". | `"","*"` (all match) |
| allowTimeSkip          | If true, two literals of the types `xsd:date` and `xsd:dateTime` with equal year, month and day part will match.                                                                                                                                                                                                  |                      |
| allowLangTagSkip       | If true, literals of the type `xsd:string` or `rdf:langString` with equal lexical value but different language tag will match.                                                                                                                                                                                    |                      |
# ABECTO Reports

Reports are defined by one SPARQL query on the result multi graph and one [Apache FreeMarker](https://freemarker.apache.org/) template, located in [src/main/resources/de/uni_jena/cs/fusion/abecto/export](src/main/resources/de/uni_jena/cs/fusion/abecto/export).
ABECTO provides the following built-in reports:

The **Deviations Report** (`deviations`) contrast the variable value of one resource with the deviating value of a corresponding resource in CSV format. In addition, it provides the aspect and the knowledge graphs of the resources, the step that mapped the resources, and an annotation snippet to mark the second value as wrong. Each entry is intended to be handled by one of the following options:
(a) fixing the value in the own knowledge graph,
(b) fixing the value in the other knowledge graph,
(c) manually fixing the mapping, or
(d) annotating the other knowledge graphs value as wrong in the comparison configuration using the provided snippet.

The **Mapping Review Report** (`mappingReview`) provides an overview of all mappings as well as all missing resources in CSV format.
It provides the aspect, the two affected knowledge graphs, the resource IRIs and labels (if applicable) the processor that provide the mapping or resource omission.
The aim of this report is to enable manual revision and adjustment of the mapping.

The **Measurements Markdown Report** (`measurementsMarkdown`) provides a tabular display of measurements on the knowledge graphs in [Markdown format](https://daringfireball.net/projects/markdown/).

The **Resource Omission Report** (`resourceOmissions`) lists all missed resources per knowledge graph in CSV format.
In addition, it provides the labels of the missing resource and the knowledge graph that contained the missing resources.

The **Wikidata Mismatch Finder Report** (`wdMismatchFinder`) provides encountered deviations in the [Mismatch Finder CSV import file format](https://github.com/wmde/wikidata-mismatch-finder/blob/main/docs/UserGuide.md\#creating-a-mismatches-import-file), provided that they can be displayed in the format.