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

## Local Execution

To see available options for execution, run:
```
java -jar target/abecto.jar --help
```
For example, to execute the tutorial configuration, run:
```
java -jar target/abecto.jar --trig result.trig src/test/resources/tutorial-configuration.trig
```

## CI Execution with Docker

Docker images for ABECTO are provide on GitHub Packages.
Inside the Docker images the ABECTO .jar file is located at `/opt/abecto.jar`, but an alias is configured allowing to type `abecto` instead of `java -jar /opt/abecto.jar`.

Example configuration for use in CI pipeline on GitLab:
```yaml
image: ghcr.io/fusion-jena/abecto:<version>

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
    container: ghcr.io/fusion-jena/abecto:<version>
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

In both examples, `<version>` needs to get replaced with an actual release number.

## License

ABECTO is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Publications

In case you use this implementation for your scientific work, please consider to cite the related paper:

* Keil, Jan Martin (2020). **[ABECTO: An ABox Evaluation and Comparison Tool for Ontologies](https://fusion.cs.uni-jena.de/fusion/publications/abecto-an-abox-evaluation-and-comparison-tool-for-ontologies/)**. In: ESWC 2020 Satellite Events: Posters and Demos. [DOI:10.1007/978-3-030-62327-2_24](https://doi.org/10.1007/978-3-030-62327-2_24).

Further related publications:

* Keil, Jan Martin (2018). **[Ontology ABox Comparison](https://fusion.cs.uni-jena.de/fusion/publications/ontology-abox-comparison/)**. In: ESWC 2018 Satellite Events: PhD Symposium. [DOI:10.1007/978-3-319-98192-5_43](https://doi.org/10.1007/978-3-319-98192-5_43).

