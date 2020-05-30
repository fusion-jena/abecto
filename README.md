[![DOI](https://zenodo.org/badge/261377020.svg)](https://zenodo.org/badge/latestdoi/261377020)
[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/zenodo/10.5281/zenodo.3786194/?filepath=abecto-tutorial.ipynb)

# ABECTO

ABECTO is an ABox Evaluation and Comparison Tool for Ontologies.

## Execution

To use ABECTO, first checkout and compile the project:
```
git clone https://github.com/fusion-jena/abecto.git
mvn -f abecto -B -Dmaven.test.skip=true install
```
Then run the ABECTO background service:
```
java -jar abecto/target/abecto.jar
```
Or use the tutorial notebook to learn the use of ABECTO. You can [run the tutorial online with Binder](https://mybinder.org/v2/zenodo/10.5281/zenodo.3786194/?filepath=abecto-tutorial.ipynb) or in your local Jupyter instance using:
```
jupyter notebook abecto/abecto-tutorial.ipynb
```

## License

[ABECTO](https://github.com/fusion-jena/abecto) is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Publications

In case you use this implementation for your scientific work, please consider to cite the related paper:

* Keil, Jan Martin (2020). **[ABECTO: An ABox Evaluation and Comparison Tool for Ontologies](https://fusion.cs.uni-jena.de/fusion/publications/abecto-an-abox-evaluation-and-comparison-tool-for-ontologies/)**. In: ESWC 2020 Satellite Events: Posters and Demos.

Further related publications:

* Keil, Jan Martin (2018). **[Ontology ABox Comparison](https://fusion.cs.uni-jena.de/fusion/publications/ontology-abox-comparison/)**. In: ESWC 2018 Satellite Events: PhD Symposium. [DOI:10.1007/978-3-319-98192-5_43](https://doi.org/10.1007/978-3-319-98192-5_43).

