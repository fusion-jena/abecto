# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.0] - 2024-03-04

*Note: This release causes a minor version increment, as the previous release wrongly caused a patch version increment only.*

### Fixed
* fix `measurementsMarkdown` report: working together with parameter `--reportOn`

## [2.1.4] - 2024-03-01

### Add
* add report `deviationsMarkdown`

### Fixed
* fix a bug that caused a `ClassCastException` in the correspondence groups stream returned by `Processor#getCorrespondenceGroups()` if a literal is present in a used predefined metadata graph 

## [2.1.3] - 2024-02-06

### Fixed
* fix `PopulationComparisonProcessor` and `PropertyComparisonProcessor`: avoid unnecessary rounding
* fix `PopulationComparisonProcessor`: align error rate parameter to `PropertyComparisonProcessor`

## [2.1.2] - 2023-12-13

### Fixed
* fix docker build

## [2.1.1] - 2023-12-09

### Added
* add benchmarks for `PopulationComparisonProcessor` and `PropertyComparisonProcessor`

### Changed
* split project into subprojects `abecto-core` and `abecto-benchmark`

## [2.1.0] - 2023-04-17

### Changed
* do not use `av:relevantResource` anymore

## [2.0.1] - 2023-04-15

### Fixed
* fix `measurementsMarkdown` report: restore `dqv:computedOn` erroneously replaced by `av:associatedDataset` 

## [2.0.0] - 2023-04-15

### Changed
* **Breaking Change**: merge `LiteralValueComparisonProcessor` and `ResourceValueComparisonProcessor` into `PropertyComparisonProcessor` to enable comparison of variables permitting literal and non-literal values
* change `PropertyComparisonProcessor`: changed absolute coverage measure and relative coverage measure to fully base on deduplicated values
* change `PropertyComparisonProcessor`: changed count measure, deduplicated count measure, absolute coverage measure and relative coverage measure to ignoring excluded values

### Fixed
* fix `PropertyComparisonProcessor`: fix handling for NaN values
* fix `PropertyComparisonProcessor`: fix deduplicated count measure to not substract duplicated values from count twice
* **Breaking Change**: use `av:associatedDataset` instead of `dqv:computedOn` for `av:MetaDataGraph` (exports for existing results will not work anymore)
* hotfix `SparqlSourceProcessor`: filter statements containing IRIs with Newline (U+000A) character to work around [DBpedia extraction framework Issue 748](https://github.com/dbpedia/extraction-framework/issues/748) and [JENA-2351](https://issues.apache.org/jira/browse/JENA-2351)

### Removed
* **Breaking Change**: removed reporting of unexpected type issues by `PropertyComparisonProcessor`
* **Breaking Change**: removed deprecated `CompletenessProcessor` (replaced by `PopulationComparisonProcessor`)

## [1.2.0] - 2023-03-09

### Fixed
* fix `LiteralValueComparisonProcessor` and `ResourceValueComparisonProcessor`: add handling for datasets not covering a compared variable
* fix `PopulationComparisonProcessor`: add handling for datasets not containing any resource for a compared aspect
* fix `PopulationComparisonProcessor`: report count value instead of deduplicated count value for count measure

### Added
* extend `LiteralValueComparisonProcessor` and `ResourceValueComparisonProcessor`: report deduplicated count measure
* extend `PopulationComparisonProcessor`: report deduplicated count measure

## [1.1.0] - 2023-02-02

### Added
* extend `LiteralValueComparisonProcessor`: add calculation of measurement count, absolute coverage, relative coverage and estimated completeness per variable
* extend `ResourceValueComparisonProcessor`: add calculation of measurement count, absolute coverage, relative coverage and estimated completeness per variable
* extend `measurementsMarkdown` export: add support for measurements with affected variable
* extend `wdMismatchFinder` export: enable reporting of missing values

### Fixed
* fix `LiteralValueComparisonProcessor`: possibly missing value deviations or value omissions in case of duplicated resources in other dataset
* fix `ResourceValueComparisonProcessor`: possibly missing value deviations or value omissions in case of duplicated resources in other dataset
* fix `LiteralValueComparisonProcessor`: possibly additional value deviations or value omissions in case of duplicated resources in the dataset
* fix `ResourceValueComparisonProcessor`: possibly additional value deviations or value omissions in case of duplicated resources in the dataset
* fix `wdMismatchFinder` export: adjust to format changes ([T288511](https://phabricator.wikimedia.org/T288511), [T313468](https://phabricator.wikimedia.org/T313468))

### Changed
* renamed `CompletenessProcessor` into `PopulationComparisonProcessor`: deprecated dummy `CompletenessProcessor` class remains to avoid a breaking change
* change `CompletenessProcessor`/`PopulationComparisonProcessor`: avoid relativeCoverage measurement in case of no values in other dataset
* change `CompletenessProcessor`/`PopulationComparisonProcessor`: increase precision of measurement results from 2 to 16 digits

## [1.0.1] - 2022-08-18

### Fixed
* fix report `wdMismatchFinder`: remove `<` and `>` around external_url values

## [1.0.0] - 2022-08-17

### Added
* extend `@Parameter` annotation: add parameter `converter` expecting an implementation of the [Jackson](https://github.com/FasterXML/jackson) `Converter` interface and make use of them during processor initialization, to enable early execution failures due to invalid parameter values

### Changed
* improve `FileSourceProcessor:` improve parser error logging
* **Breaking Change**: renamed `FBRuleReasoningProcessor` into `ForwardRuleReasoningProcessor`
* **Breaking Change**: `SparqlConstructProcessor` and `SparqlSourceProcessor` expect datatype `xsd:string` instead of `av:SparqlQuery` for parameter `query`, to ease configuration writing
* **Breaking Change**: `av:VariablePath`s of aspects expect datatype `xsd:string` instead of `av:SparqlPropertyPath` for the property `av:propertyPath`

### Removed
* **Breaking Change**: removed support for the RDF datatypes `av:SparqlPropertyPath` and `av:SparqlQuery`
* remove custom xsd:dateTimeStamp mapping

## [0.10.0] - 2022-07-19

### Added
* extend report `deviations`: add column `snippetToAnnotateValueComparedToAsWrong` to ease wrong value annotation for future plan executions

### Fixed
* fix `Step`: consider associated dataset of predefined metadata graphs
* fix report engine: avoid character escaping in of literals

## [0.9.1] - 2022-07-15

### Fixed
* fix `wdMismatchFinder` report: remove datatypes of wikidata_value and external_value

## [0.9.0] - 2022-07-15

### Added
* add result export template `measurementsMarkdown`

### Fixed
* fix `wdMismatchFinder` report: exclude not yet supported mismatches of [(alternative) labels or missing values](https://phabricator.wikimedia.org/T312767) and [qualifier values](https://phabricator.wikimedia.org/T312851)

## [0.8.0] - 2022-07-01

### Added
* extend `LiteralValueComparisonProcessor`: add parameter `allowLangTagSkip` to enable comparison of values from sources using and not using language tags

## [0.7.2] - 2022-06-30

## Fixed
* fix all reports

## [0.7.1] - 2022-06-30

## Fixed
* fix `UsePresentMappingProcessor`: fix logging
* fix `CompletenessProcessor`: add handling of missing aspect coverage by a datasets
* fix `LiteralValueComparisonProcessor`: add handling of missing aspect coverage by a datasets
* fix `ResourceValueComparisonProcessor`: add handling of missing aspect coverage by a datasets
* fix `JaroWinklerMappingProcessor`: add handling of missing aspect coverage by a datasets
* fix `JaroWinklerMappingProcessor`: update similarity library fixing a bug that might cause a lower similarity between resources with several values of the compared variable
* fix all reports: disable special character escaping
* hotfix for https://issues.apache.org/jira/browse/JENA-2335 

## [0.7.0] - 2022-06-24

### Added
* extend `FileSourceProcessor`: permit multiple `path` parameter values

### Changed
* rename `RdfFileSourceProcessor` into `FileSourceProcessor`
* improve `UsePresentMappingProcessor`: improve logging on missing aspect patterns

## [0.6.0] - 2022-06-23

### Added
* add CLI parameters `--failOnDeviation`, `--failOnValueOmission`, `--failOnResourceOmission`, `--failOnWrongValue` and `--failOnIssue` to enable exit code 1 in case of deviations/value omissions/resource omissions/wrong values/other issues
* add CLI parameter `--reportOn` to enable limited scope of reports and exit codes to a single dataset
* add result export template `mappingReview`
* extend `UrlSourceProcessor`: permit multiple `url` parameter values
* extend result export template `resourceOmission`: sort results, add optional rdfs:label

### Fixed
* fix result export template `deviations`: remove duplicated columns
* fix `UrlSourceProcessor`: use correct URL for request in heuristic language detection mode; enable followRedirects in brute force language detection mode
* fix all reports: include report templates into JAR

### Changed
* TRIG output avoids base and empty prefix to ease result reading

## [0.5.0] - 2022-05-12

### Added
* add `FBRuleReasoningProcessor`: Generates statements using custom [rules](https://jena.apache.org/documentation/inference/#RULEhybrid)
* extend `SparqlSourceProcessor`: add retries on failures configurable with parameters `chunkSizeDecreaseFactor` and `maxRetries`
* extend `SparqlSourceProcessor`: add parameter `followInverseUnlimited`
* extend `SparqlSourceProcessor`: add parameter `ignoreInverse`
* extend `SparqlSourceProcessor`: add `rdf:first` and `rdf:rest` to default `followUnlimited` values
* extend `LiteralValueComparisonProcessor`: add parameter `languageFilterPatterns`
* extend `LiteralValueComparisonProcessor`: add parameter `allowTimeSkip` to enable date part of `xsd:date` and `xsd:dateTime`
* extend `MappingProcessor`: add persisting of transitive correspondences
* extend `Parameters`: enable use of different Collection subtypes for Processor Parameters
* extend `Aspect`: enable use of one pattern for multiple datasets
* extend logging: log `Step` processing start and completion
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
* fix `EquivalentValueMappingProcessor`: fix message format
* fix `EquivalentValueMappingProcessor`: skip resource with unbound variables
* fix `SparqlSourceProcessor`: enable arbitrary query lengths by updating Apache Jena fixing [JENA-2257](https://issues.apache.org/jira/browse/JENA-2257)
* fix `SparqlSourceProcessor`: fix expected datatypes of some parameters
* fix `SparqlSourceProcessor`: increased compatibility to SPARQL endpoint implementations
* improve performance of several mapping and comparison processors

### Changed
* changed logging format
* changed `UsePresentMappingProcessor`: replaced parameter `assignmentPaths` expecting SPARQL Property Paths with parameter `variable` expecting an aspect variable name

## [0.4.0] - 2022-01-12

### Added
* add `EquivalentValueMappingProcessor`: Provides correspondences based on equivalent values.

### Changed
* rename vocabulary resource av:SparqlSelectQuery into av:SparqlQuery

### Fixed
* fix `UrlSourceProcessor`: parameter value can now be set
* fix `UrlSourceProcessor`: uses accept headers to explicitly request RDF in case of Content Negotiation
* fix output av:relevantResource statements in case of blank node aspects

## [0.3.0] - 2022-01-10

### Changed
* **transform ABECTO from a webservice into a command line tool** with RDF dataset files as input and output
* merge `CategoryCountProcessor` into `CompletenessProcessor`
* rename `RelationalMappingProcessor` into `FunctionalMappingProcessor`
* rename `LiteralDeviationProcessor` into `LiteralValueComparisonProcessor`
* rename `ResourceDeviationProcessor` into `ResourceValueComparisonProcessor`
* rename **categories** into **aspects**
* rename **mappings** into **correspondences**

### Added
* add `SparqlSourceProcessor`: Extracts RDF from a SPARQL endpoint.

### Removed
* remove `ManualMappingProcessor`: Predefined correspondences can be stated in the configuration directly.
* remove `ManualCategoryProcessor`: Aspects can be stated in the configuration directly.
* remove `TransitiveMappingProcessor`: Transitive correspondences are now added automatically
* remove **Jupyter Notebook support** to control ABECTO
* remove `OpenlletReasoningProcessor`: Enable publishing as packed binary version

## [0.2.1] - 2020-12-03

### Fixed
* fix HTML output in Jupyter Notebooks: resolve misplaced `</div>`

## [0.2.0] - 2020-12-03

### Added
* add `UrlSourceProcessor`: Loads an RDF document from a URL.
* add `ExecutionRestController#getMetadata`: return metadata of loaded sources used in this execution
* add `UsePresentMappingProcessor`: Provides mappings for resources connected in the ontologies with given property paths.
* add `TransitiveMappingProcessor`: Provides transitive closure of existing mappings.
* add `CompletenessProcessor`: Provides absolute and relative coverage statistics, omission detection, and duplicate detection of resources by category and ontologies.
* extend `SparqlConstructProcessor`: enable recursive generation of new triples with SPARQL Construct Query and add parameter `maxIterations` with default value `1`
* extend **Measurement Report** for Jupyter Notebooks: alphabetical order of measurements, alphabetical order of dimensions, replace ontology UUIDs with ontology names in dimension columns
* add **Omission Report** for Jupyter Notebooks
* extend `JaroWinklerMappingProcessor`: add parameter `defaultLangTag` used as fallback locale for LowerCase conversion during case-insensitive mapping
* add `/version` API call returning the version of ABECTO
* add **Mapping Report** in Jupyter Notebooks: replacing heavy-weighted *Mapping Review*

### Fixed
* fix `JaroWinklerMappingProcessor`: ignore other categories, enable case-insensitive mapping
* fix `Category`: `getPatternVariables()` does not anymore return helper `Var` for BlankNodePropertyLists and BlankNodePropertyListPaths introduced by Apache Jena, which cause Exceptions in `CategoryCountProcessor`
* fix **Measurement** and **Omission**: use `abecto:ontology` instead of `abecto:knowledgeBase`
* fix **Measurement Report** in Jupyter Notebooks: no dimensions column header concatenation of multiple measurement types
* fix `AbstractRefinementProcessor`: disable RDFS reasoning on input ontologies
* fix `LiteralDeviationProcessor`: correct handling of float and double, enable multiple values of same property
* fix `Deviation Report` in Jupyter Notebooks: solve omission of deviations
* fix HTML output in Jupyter Notebooks: add line-breaks to enable `git diff` for result

### Removed
* remove `Mapping Review` in Jupyter Notebooks: replaced by simple *Mapping Report*

## [0.1.1] - 2020-05-29

### Fixed
* fix `LiteralDeviationProcessor`: support numerical value Infinite, correct mixed numeric type comparison, address precision issues of mixed number type comparison
* fix `Deviation Report` in Jupyter Notebooks: added missing IRI of resources with 2 or more deviations
* fix `RelationalMappingProcessor`: skip candidates with missing value for a variable, fix missing mappings in case of at least 3 incomplete ontologies
* fix `ExecutionRestController#getData`: include transformation nodes data
* fix `ManualCategoryProcessor`: allow empty parameters

## [0.1.0] - 2020-05-05

### Added
* add `RdfFileSourceProcessor`: Loads an RDF document from the local file system.
* add `JaroWinklerMappingProcessor`: Provides mappings based on the Jaro-Winkler Similarity of string property values using our implementation from [Efficient Bounded Jaro-Winkler Similarity Based Search](https://doi.org/10.18420/btw2019-13).
* add `ManualMappingProcessor`: Enables users to manually adjust the mappings by providing or suppressing mappings.
* add `RelationalMappingProcessor`: Provides mappings based on the mappings of referenced resources.
* add `OpenlletReasoningProcessor`: Infers the logical consequences of the input RDF models utilizing the [Openllet Reasoner](https://github.com/Galigator/openllet) to generate additional triples.
* add `SparqlConstructProcessor`: Applies a given SPARQL Construct Query to the input RDF models to generate additional triples.
* add `CategoryCountProcessor`: Measures the number of resources and property values per category.
* add `LiteralDeviationProcessor`: Detects deviations between the property values of mapped resources as defined in the categories.
* add `ManualCategoryProcessor`: Enables users to manually define resource categories and their properties.
* add `ResourceDeviationProcessor`: Detects deviations between the resource references of mapped resources as defined in the categories.

[Unreleased]: https://github.com/fusion-jena/abecto/compare/v2.2.0...HEAD
[2.2.0]: https://github.com/fusion-jena/abecto/compare/v2.1.4...v2.2.0
[2.1.4]: https://github.com/fusion-jena/abecto/compare/v2.1.3...v2.1.4
[2.1.3]: https://github.com/fusion-jena/abecto/compare/v2.1.2...v2.1.3
[2.1.2]: https://github.com/fusion-jena/abecto/compare/v2.1.1...v2.1.2
[2.1.1]: https://github.com/fusion-jena/abecto/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/fusion-jena/abecto/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/fusion-jena/abecto/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/fusion-jena/abecto/compare/v1.2.0...v2.0.0
[1.2.0]: https://github.com/fusion-jena/abecto/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/fusion-jena/abecto/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/fusion-jena/abecto/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/fusion-jena/abecto/compare/v0.10.0...v1.0.0
[0.10.0]: https://github.com/fusion-jena/abecto/compare/v0.9.1...v0.10.0
[0.9.1]: https://github.com/fusion-jena/abecto/compare/v0.9.0...v0.9.1
[0.9.0]: https://github.com/fusion-jena/abecto/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/fusion-jena/abecto/compare/v0.7.2...v0.8.0
[0.7.2]: https://github.com/fusion-jena/abecto/compare/v0.7.1...v0.7.2
[0.7.1]: https://github.com/fusion-jena/abecto/compare/v0.7.0...v0.7.1
[0.7.0]: https://github.com/fusion-jena/abecto/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/fusion-jena/abecto/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/fusion-jena/abecto/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/fusion-jena/abecto/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/fusion-jena/abecto/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/fusion-jena/abecto/compare/v0.2...v0.2.1
[0.2.0]: https://github.com/fusion-jena/abecto/compare/v0.1.1...v0.2
[0.1.1]: https://github.com/fusion-jena/abecto/compare/v0.1...v0.1.1
[0.1.0]: https://github.com/fusion-jena/abecto/releases/tag/v0.1
