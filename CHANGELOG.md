# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
* add **UrlSourceProcessor**: Loads an RDF document from a URL.

## [v0.1.1] - 2020-05-29

### Fixed
* fix **LiteralDeviationProcessor**: support numerical value Infinite, correct mixed numeric type comparison, address precision issues of mixed number type comparison
* fix **Deviation Report** in Jupyter Notebooks: added missing IRI of resources with 2 or more deviations
* fix **RelationalMappingProcessor**: skip candidates with missing value for a variable, fix missing mappings in case of at least 3 incomplete ontologies
* fix **ExecutionRestController#getData**: include transformation nodes data
* fix **ManualCategoryProcessor**: allow empty parameters

## [v0.1] - 2020-05-05

### Added
* add **RdfFileSourceProcessor**: Loads an RDF document from the local file system.
* add **JaroWinklerMappingProcessor**: Provides mappings based on the Jaro-Winkler Similarit of string property values using our implementation from [Efficient Bounded Jaro-Winkler Similarity Based Search](https://doi.org/10.18420/btw2019-13).
* add **ManualMappingProcessor**: Enables users to manually adjust the mappings by providing or suppressing mappings.
* add **RelationalMappingProcessor**: Provides mappings based on the mappings of referenced resources.
* add **OpenlletReasoningProcessor**: Infers the logical consequences of the input RDF models utilizing the [Openllet Reasoner](https://github.com/Galigator/openllet) to generate additional triples.
* add **SparqlConstructProcessor**: Applies a given SPARQL Construct Query to the input RDF models to generate additional triples.
* add **CategoryCountProcessor**: Measures the number of resources and property values per category.
* add **LiteralDeviationProcessor**: Detects deviations between the property values of mapped resources as defined in the categories.
* add **ManualCategoryProcessor**: Enables users to manually define resource categories and their properties.
* add **ResourceDeviationProcessor**: Detects deviations between the resource references of mapped resources as defined in the categories.
