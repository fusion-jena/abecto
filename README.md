[![DOI](https://zenodo.org/badge/261377020.svg)](https://zenodo.org/badge/latestdoi/261377020)

# ABECTO

ABECTO is an ABox Evaluation and Comparison Tool for Ontologies.

## Usage

To use ABECTO, first checkout the project and compile ABECTO using Maven:
```
mvn -B -Dmaven.test.skip=true package
```
This will create a stand alone .jar file at [target/abecto.jar](target).
To see available options for execution, run:
```
java -jar target/abecto.jar --help
```
For example, to execute the tutorial configuration, run:
```
java -jar target/abecto.jar src/test/resources/tutorial-configuration.trig result.trig
```
The execution of ABECTO is configured in a configuration file, which is a RDF dataset file ([TriG](https://www.w3.org/TR/trig/), [N-Quads](https://www.w3.org/TR/n-quads/), …), using the ABECTO Vocabulary. For an example see the [tutorial configuration](src/test/resources/tutorial-configuration.trig). Further build in processors can be found in [src/main/java/de/uni_jena/cs/fusion/abecto/processor/](src/main/java/de/uni_jena/cs/fusion/abecto/processor/).

## License

ABECTO is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Publications

In case you use this implementation for your scientific work, please consider to cite the related paper:

* Keil, Jan Martin (2020). **[ABECTO: An ABox Evaluation and Comparison Tool for Ontologies](https://fusion.cs.uni-jena.de/fusion/publications/abecto-an-abox-evaluation-and-comparison-tool-for-ontologies/)**. In: ESWC 2020 Satellite Events: Posters and Demos. [DOI:10.1007/978-3-030-62327-2_24](https://doi.org/10.1007/978-3-030-62327-2_24).

Further related publications:

* Keil, Jan Martin (2018). **[Ontology ABox Comparison](https://fusion.cs.uni-jena.de/fusion/publications/ontology-abox-comparison/)**. In: ESWC 2018 Satellite Events: PhD Symposium. [DOI:10.1007/978-3-319-98192-5_43](https://doi.org/10.1007/978-3-319-98192-5_43).

