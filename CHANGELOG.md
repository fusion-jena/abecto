# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## Fixed
* fix **UsePresentMappingProcessor**: fix logging

## [0.7.0] - 2022-06-24

### Added
* extend **FileSourceProcessor**: permit multiple `path` parameter values

### Changed
* rename **RdfFileSourceProcessor** into **FileSourceProcessor**

## [0.6.0] - 2022-06-23

### Added
* add CLI parameters `--failOnDeviation`, `--failOnValueOmission`, `--failOnResourceOmission`, `--failOnWrongValue` and `--failOnIssue` to enable exit code 1 in case of deviations/value omissions/resource omissions/wrong values/other issues
* add CLI parameter `--reportOn` to enable limited scope of reports and exit codes to a single dataset
* add result export template `mappingReview`
* extend **UrlSourceProcessor**: permit multiple `url` parameter values
* extend result export template `resourceOmission`: sort results, add optional rdfs:label

### Fixed
* fix result export template `deviations`: remove duplicated columns
* fix **UrlSourceProcessor**: use correct URL for request in heuristic language detection mode; enable followRedirects in brute force language detection mode
* fix all reports: include report templates into JAR

### Changed
* TRIG output avoids base and empty prefix to ease result reading

## [0.5.0] - 2022-05-12

### Added
* add **FBRuleReasoningProcessor**: Generates statements using custom [rules](https://jena.apache.org/documentation/inference/#RULEhybrid)
* extend **SparqlSourceProcessor**: add retries on failures configurable with parameters `chunkSizeDecreaseFactor` and `maxRetries`
* extend **SparqlSourceProcessor**: add parameter `followInverseUnlimited`
* extend **SparqlSourceProcessor**: add parameter `ignoreInverse`
* extend **SparqlSourceProcessor**: add `rdf:first` and `rdf:rest` to default `followUnlimited` values
* extend **LiteralValueComparisonProcessor**: add parameter `languageFilterPatterns`
* extend **LiteralValueComparisonProcessor**: add parameter `allowTimeSkip` to enable date part of `xsd:date` and `xsd:dateTime`
* extend **MappingProcessor**: add persisting of transitive correspondences
* extend **Parameters**: enable use of different Collection subtypes for Processor Parameters
* extend **Aspect**: enable use of one pattern for multiple datasets
* extend logging: log **Step** processing start and completion
* extend logging: log CLI execution phases
* extend built in documentation (`--help`)
* add result export engine
* add result export template `deviations`
* add result export template `resourceOmissions`
* add result export template `wdMismatchFinder` (see [Wikidata Mismatch Finder file format](https://github.com/wmde/wikidata-mismatch-finder/blob/main/docs/UserGuide.md#creating-a-mismatches-import-file]))
* add extraction of property path between key variable and other variables to make them available for exports
* add reuse of sources prefix definitions for the output file
* add CLI parameter `--loadOnly` to enable reuse of execution output for report generation

### Fixed
* fix **EquivalentValueMappingProcessor**: fix message format
* fix **EquivalentValueMappingProcessor**: skip resource with unbound variables
* fix **SparqlSourceProcessor**: enable arbitrary query lengths by updating Apache Jena fixing [JENA-2257](https://issues.apache.org/jira/browse/JENA-2257)
* fix **SparqlSourceProcessor**: fix expected datatypes of some parameters
* fix **SparqlSourceProcessor**: increased compatibility to SPARQL endpoint implementations
* improve performance of several mapping and comparison processors

### Changed
* changed logging format
* changed **UsePresentMappingProcessor**: replaced parameter `assignmentPaths` expecting SPARQL Property Paths with parameter `variable` expecting an aspect variable name

## [0.4.0] - 2022-01-12

### Added
* add **EquivalentValueMappingProcessor**: Provides correspondences based on equivalent values.

### Changed
* rename vocabulary resource av:SparqlSelectQuery into av:SparqlQuery

### Fixed
* fix **UrlSourceProcessor**: parameter value can now be set
* fix **UrlSourceProcessor**: uses accept headers to explicitly request RDF in case of Content Negotiation
* fix output av:relevantResource statements in case of blank node aspects

## [0.3.0] - 2022-01-10

### Changed
* **transform ABECTO from a webservice into a command line tool** with RDF dataset files as input and output
* merge **CategoryCountProcessor** into **CompletenessProcessor**
* rename **RelationalMappingProcessor** into **FunctionalMappingProcessor**
* rename **LiteralDeviationProcessor** into **LiteralValueComparisonProcessor**
* rename **ResourceDeviationProcessor** into **ResourceValueComparisonProcessor**
* rename **categories** into **aspects**
* rename **mappings** into **correspondences**

### Added
* add **SparqlSourceProcessor**: Extracts RDF from a SPARQL endpoint.

### Removed
* remove **ManualMappingProcessor**: Predefined correspondences can be stated in the configuration directly.
* remove **ManualCategoryProcessor**: Aspects can be stated in the configuration directly.
* remove **TransitiveMappingProcessor**: Transitive correspondences are now added automatically
* remove **Jupyter Notebook support** to control ABECTO
* remove **OpenlletReasoningProcessor**: Enable publishing as packed binary version

## [0.2.1] - 2020-12-03

### Fixed
* fix HTML output in Jupyter Notebooks: resolve misplaced `</div>`

## [0.2.0] - 2020-12-03

### Added
* add **UrlSourceProcessor**: Loads an RDF document from a URL.
* add **ExecutionRestController#getMetadata**: return metadata of loaded sources used in this execution
* add **UsePresentMappingProcessor**: Provides mappings for resources connected in the ontologies with given property paths.
* add **TransitiveMappingProcessor**: Provides transitive closure of existing mappings.
* add **CompletenessProcessor**: Provides absolute and relative coverage statistics, omission detection, and duplicate detection of resources by category and ontologies.
* extend **SparqlConstructProcessor**: enable recursive generation of new triples with SPARQL Construct Query and add parameter `maxIterations` with default value `1`
* extend **Measurement Report** for Jupyter Notebooks: alphabetical order of measurements, alphabetical order of dimensions, replace ontology UUIDs with ontology names in dimension columns
* add **Omission Report** for Jupyter Notebooks
* extend **JaroWinklerMappingProcessor**: add parameter `defaultLangTag` used as fallback locale for LowerCase conversion during case-insensitive mapping
* add `/version` API call returning the version of ABECTO
* add **Mapping Report** in Jupyter Notebooks: replacing heavy-weighted *Mapping Review*

### Fixed
* fix **JaroWinklerMappingProcessor**: ignore other categories, enable case-insensitive mapping
* fix **Category**: `getPatternVariables()` does not anymore return helper `Var` for BlankNodePropertyLists and BlankNodePropertyListPaths introduced by Apache Jena, which cause Exceptions in **CategoryCountProcessor**
* fix **Measurement** and **Omission**: use `abecto:ontology` instead of `abecto:knowledgeBase`
* fix **Measurement Report** in Jupyter Notebooks: no dimensions column header concatenation of multiple measurement types
* fix **AbstractRefinementProcessor**: disable RDFS reasoning on input ontologies
* fix **LiteralDeviationProcessor**: correct handling of float and double, enable multiple values of same property
* fix **Deviation Report** in Jupyter Notebooks: solve omission of deviations
* fix HTML output in Jupyter Notebooks: add line-breaks to enable `git diff` for result

### Removed
* remove **Mapping Review** in Jupyter Notebooks: replaced by simple *Mapping Report*

## [0.1.1] - 2020-05-29

### Fixed
* fix **LiteralDeviationProcessor**: support numerical value Infinite, correct mixed numeric type comparison, address precision issues of mixed number type comparison
* fix **Deviation Report** in Jupyter Notebooks: added missing IRI of resources with 2 or more deviations
* fix **RelationalMappingProcessor**: skip candidates with missing value for a variable, fix missing mappings in case of at least 3 incomplete ontologies
* fix **ExecutionRestController#getData**: include transformation nodes data
* fix **ManualCategoryProcessor**: allow empty parameters

## [0.1.0] - 2020-05-05

### Added
* add **RdfFileSourceProcessor**: Loads an RDF document from the local file system.
* add **JaroWinklerMappingProcessor**: Provides mappings based on the Jaro-Winkler Similarity of string property values using our implementation from [Efficient Bounded Jaro-Winkler Similarity Based Search](https://doi.org/10.18420/btw2019-13).
* add **ManualMappingProcessor**: Enables users to manually adjust the mappings by providing or suppressing mappings.
* add **RelationalMappingProcessor**: Provides mappings based on the mappings of referenced resources.
* add **OpenlletReasoningProcessor**: Infers the logical consequences of the input RDF models utilizing the [Openllet Reasoner](https://github.com/Galigator/openllet) to generate additional triples.
* add **SparqlConstructProcessor**: Applies a given SPARQL Construct Query to the input RDF models to generate additional triples.
* add **CategoryCountProcessor**: Measures the number of resources and property values per category.
* add **LiteralDeviationProcessor**: Detects deviations between the property values of mapped resources as defined in the categories.
* add **ManualCategoryProcessor**: Enables users to manually define resource categories and their properties.
* add **ResourceDeviationProcessor**: Detects deviations between the resource references of mapped resources as defined in the categories.

[Unreleased]: https://github.com/fusion-jena/abecto/compare/v0.7.0...HEAD
[0.7.0]: https://github.com/fusion-jena/abecto/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/fusion-jena/abecto/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/fusion-jena/abecto/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/fusion-jena/abecto/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/fusion-jena/abecto/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/fusion-jena/abecto/compare/v0.2...v0.2.1
[0.2.0]: https://github.com/fusion-jena/abecto/compare/v0.1.1...v0.2
[0.1.1]: https://github.com/fusion-jena/abecto/compare/v0.1...v0.1.1
[0.1.0]: https://github.com/fusion-jena/abecto/releases/tag/v0.1
