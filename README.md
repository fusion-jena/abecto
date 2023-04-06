[![Tests](https://github.com/fusion-jena/abecto/actions/workflows/maven.yml/badge.svg)](https://github.com/fusion-jena/abecto/actions/workflows/maven.yml)
[![DOI](https://zenodo.org/badge/261377020.svg)](https://zenodo.org/badge/latestdoi/261377020)

# ABECTO

ABECTO is an ABox Evaluation and Comparison Tool for Ontologies.

<!-- TOC -->
* [ABECTO](#abecto)
  * [Building](#building)
  * [Configuration](#configuration)
    * [How to write an ABECTO plan?](#how-to-write-an-abecto-plan)
  * [Execution](#execution)
  * [CI Usage with Docker](#ci-usage-with-docker)
  * [Project Examples](#project-examples)
  * [License](#license)
  * [Publications](#publications)
<!-- TOC -->

## Building

To use ABECTO, first checkout the project and compile ABECTO using Maven:

```shell
mvn -B -Dmaven.test.skip=true package
```

This will create a stand alone .jar file at [target/abecto.jar](target).

## Configuration

The execution of ABECTO is configured in a plan file, which is an RDF dataset file ([TriG](https://www.w3.org/TR/trig/), [N-Quads](https://www.w3.org/TR/n-quads/), …), using the [ABECTO Vocabulary](http://w3id.org/abecto/vocabulary). For an example see the [tutorial configuration](src/test/resources/tutorial-configuration.trig). Further build in processors can be found in [src/main/java/de/uni_jena/cs/fusion/abecto/processor/](src/main/java/de/uni_jena/cs/fusion/abecto/processor/).

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

