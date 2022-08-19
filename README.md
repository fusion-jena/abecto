[![DOI](https://zenodo.org/badge/261377020.svg)](https://zenodo.org/badge/latestdoi/261377020)

# ABECTO

ABECTO is an ABox Evaluation and Comparison Tool for Ontologies.

## Building

To use ABECTO, first checkout the project and compile ABECTO using Maven:

```
mvn -B -Dmaven.test.skip=true package
```

This will create a stand alone .jar file at [target/abecto.jar](target).

## Configuration

The execution of ABECTO is configured in a configuration file, which is a RDF dataset file ([TriG](https://www.w3.org/TR/trig/), [N-Quads](https://www.w3.org/TR/n-quads/), …), using the [ABECTO Vocabulary](http://w3id.org/abecto/vocabulary). For an example see the [tutorial configuration](src/test/resources/tutorial-configuration.trig). Further build in processors can be found in [src/main/java/de/uni_jena/cs/fusion/abecto/processor/](src/main/java/de/uni_jena/cs/fusion/abecto/processor/).

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
  ```
  java -jar target/abecto.jar --help
  ```
* run the tutorial plan and store the result:
  ```
  java -jar target/abecto.jar --trig result.trig src/test/resources/tutorial-configuration.trig
  ```
* create an deviations report for a specific dataset and without re-running the plan:
  ```
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

