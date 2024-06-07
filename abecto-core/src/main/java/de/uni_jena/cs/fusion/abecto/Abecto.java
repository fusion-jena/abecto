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

package de.uni_jena.cs.fusion.abecto;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.util.Datasets;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(description = "Compares and evaluates several RDF datasets.", name = "abecto", mixinStandardHelpOptions = true, versionProvider = Abecto.ManifestVersionProvider.class)
public class Abecto implements Callable<Integer> {

	static {
		// configure logging
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$-7s [%3$s] %5$s %6$s%n");
		java.util.logging.Logger.getLogger("org.apache.jena.riot").setLevel(Level.WARNING);
		java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.ALL);

		// init Apache Jena
		initApacheJena();
	}

	final static Logger log = LoggerFactory.getLogger(Abecto.class);

	public static void main(String... args) {
		int exitCode = new CommandLine(new Abecto()).execute(args);
		System.exit(exitCode);
	}

	@Option(names = { "-p",
			"--plan" }, paramLabel = "IRI", description = "IRI of the plan to process. Required, if the configuration contains multiple plans.")
	String planIri;

	@Option(names = "--trig", paramLabel = "FILE", description = "RDF TRIG dataset file for the execution results.")
	File trigOutputFile;

	@Option(names = "--loadOnly", description = "If set, the plan will not get executed. This enables to export results without repeated plan execution.")
	boolean loadOnly;

	@Option(names = { "-E",
			"--export" }, paramLabel = "TEMPLATE_NAME=FILE", description = "Template and output file for an result export. Can be set multiple times.")
	Map<String, File> exports;

	@Option(names = "--reportOn", paramLabel = "IRI", description = "IRI of the source to report on. Reports will be limited to results about this source.")
	String sourceToReportOn;

	@Option(names = "--failOnDeviation", description = "If set, a exit code > 0 will be returned, if the results contain a deviation. Useful together with \"--reportOn\".")
	boolean failOnDeviation;

	@Option(names = "--failOnValueOmission", description = "If set, a exit code > 0 will be returned, if the results contain a value omission. Useful together with \"--reportOn\".")
	boolean failOnValueOmission;

	@Option(names = "--failOnResourceOmission", description = "If set, a exit code > 0 will be returned, if the results contain a resource omission. Useful together with \"--reportOn\".")
	boolean failOnResourceOmission;

	@Option(names = "--failOnWrongValue", description = "If set, a exit code > 0 will be returned, if the results contain a wrong value. Useful together with \"--reportOn\".")
	boolean failOnWrongValue;

	@Option(names = "--failOnIssue", description = "If set, a exit code > 0 will be returned, if the results contain an issue. Useful together with \"--reportOn\".")
	boolean failOnIssue;

	@Parameters(index = "0", paramLabel = "FILE", description = "RDF dataset file containing the plan configuration and optionally plan execution results (see --loadOnly).")
	File planFile;

	private Dataset datasetForExecution = DatasetFactory.createGeneral();
	private Dataset datasetForReporting;
	private File relativePathBase;
	private Configuration freemarker;
	private final static String TEMPLATE_FOLDER = "/de/uni_jena/cs/fusion/abecto/export";
	private final static String VOCABULARY_FOLDER = "/de/uni_jena/cs/fusion/abecto/vocabulary";

	@Override
	public Integer call() {
		try {
			determineRelativePathBase();
			loadPlanFile();
			executePlanIfConfigured();
			reusePlanPrefixesForResults();
			writeResultTrigFileIfConfigured();
			prepareDatasetForReporting();
			generateAndWriteReportsAsConfigured();
			return determineExitCode();
		} catch (CompletionException e) {
			log.error("Plan execution failed.", e.getCause());
			return 1;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return 1;
		}
	}

	private void determineRelativePathBase() {
		relativePathBase = planFile.getParentFile();
	}

	private void loadPlanFile()
			throws IllegalArgumentException, IOException {
		log.info("Loading plan dataset file started.");
		Datasets.read(datasetForExecution, new FileInputStream(planFile));
		log.info("Loading plan dataset file completed.");
	}

	public void reusePlanPrefixesForResults() {
		PrefixMapping datasetPrefixMapping = datasetForExecution.getPrefixMapping();
		datasetPrefixMapping.setNsPrefixes(datasetForExecution.getDefaultModel().getNsPrefixMap());
		datasetForExecution.listModelNames().forEachRemaining(
				modelName -> datasetPrefixMapping.setNsPrefixes(datasetForExecution.getNamedModel(modelName).getNsPrefixMap()));
		// no empty prefix to ease result reading
		datasetPrefixMapping.removeNsPrefix("");
	}

	private void writeResultTrigFileIfConfigured() throws FileNotFoundException {
		if (trigOutputFile != null && !loadOnly) {
			log.info("Writing plan execution results as TRIG file started.");
			RDFWriter.source(datasetForExecution).base(null) // no base prefix to ease result reading
					.format(RDFFormat.TRIG_PRETTY).output(new FileOutputStream(trigOutputFile));
			log.info("Writing plan execution results as TRIG file completed.");
		}
	}

	private void executePlanIfConfigured() throws ReflectiveOperationException {
		if (!loadOnly) {
			log.info("Plan execution started.");
			executePlan(planIri);
			log.info("Plan execution completed.");
		} else {
			log.info("Plan execution skipped.");
		}
	}

	private void prepareDatasetForReporting() {
		if (sourceToReportOn == null) {
			datasetForReporting = datasetForExecution;
		} else {
			datasetForReporting = getDatasetAboutSourceToReportOn();
		}
		loadOntologyForReporting("http://w3id.org/abecto/vocabulary", "abecto-vocabulary.ttl", "TTL");
		loadOntologyForReporting("http://www.ontology-of-units-of-measure.org/resource/om-2", "om-2.0.rdf", "RDF/XML");
	}

	private void generateAndWriteReportsAsConfigured() throws TemplateException, IOException {
		if (exports != null) {
			// prepare freemarker configuration
			freemarker = new Configuration(Configuration.VERSION_2_3_31);
			freemarker.setClassForTemplateLoading(this.getClass(), TEMPLATE_FOLDER);
			freemarker.setDefaultEncoding("UTF-8");
			freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			freemarker.setLogTemplateExceptions(false);
			freemarker.setWrapUncheckedExceptions(true);
			freemarker.setFallbackOnNullLoopVariable(false);
			// apply export templates
			for (Entry<String, File> export : exports.entrySet()) {
				log.info(String.format("Export with template \"%s\" started.", export.getKey()));
				export(export.getKey(), export.getValue(), datasetForReporting);
				log.info(String.format("Export with template \"%s\" completed.", export.getKey()));
			}
		}
	}

	private int determineExitCode() {
		int exitCode = 0;
		if (failOnDeviation && resultsForReportingContainIssue(AV.Deviation)) {
			exitCode += 2;
		}
		if (failOnValueOmission && resultsForReportingContainIssue(AV.ValueOmission)) {
			exitCode += 4;
		}
		if (failOnResourceOmission && resultsForReportingContainIssue(AV.ResourceOmission)) {
			exitCode += 8;
		}
		if (failOnWrongValue && resultsForReportingContainIssue(AV.WrongValue)) {
			exitCode += 16;
		}
		if (failOnIssue && resultsForReportingContainIssue(AV.Issue)) {
			exitCode += 32;
		}
		return exitCode;
	}

	private void loadOntologyForReporting(String graphIri, String fileName, String lang) {
		datasetForReporting.addNamedModel(graphIri, ModelFactory.createDefaultModel()
				.read(this.getClass().getResourceAsStream(VOCABULARY_FOLDER + "/" + fileName), null, lang));
	}

	private boolean resultsForReportingContainIssue(Resource affectedBy) {
		return datasetForReporting.getUnionModel().contains(null, RDF.type, affectedBy);
	}

	private Dataset getDatasetAboutSourceToReportOn() {
		Model configurationModel = datasetForExecution.getDefaultModel();
		Resource datasetToReportOnResource = ResourceFactory.createResource(sourceToReportOn);
		Dataset modelsForDataset = DatasetFactory.create(configurationModel);
		datasetForExecution.listModelNames().forEachRemaining(modelResource -> {
			if (configurationModel.contains(modelResource, RDF.type, AV.PrimaryDataGraph)
					|| (configurationModel.contains(modelResource, RDF.type, AV.MetaDataGraph)
							&& (!configurationModel.contains(modelResource, AV.associatedDataset) || configurationModel
									.contains(modelResource, AV.associatedDataset, datasetToReportOnResource)))) {
				modelsForDataset.addNamedModel(modelResource, datasetForExecution.getNamedModel(modelResource));
			}
		});
		return modelsForDataset;
	}

	public void export(String exportType, File outputFile, Dataset reportOn) throws IOException, TemplateException {
		Template template = this.freemarker.getTemplate(exportType + ".ftl");
		String queryStr = new String(
				this.getClass().getResourceAsStream(TEMPLATE_FOLDER + "/" + exportType + ".rq").readAllBytes(),
				StandardCharsets.UTF_8);
		List<Map<String, String>> data = new ArrayList<>();
		ResultSet results = QueryExecutionFactory.create(queryStr, reportOn).execSelect();
		results.forEachRemaining(binding -> {
			Map<String, String> map = new HashMap<>();
			data.add(map);
			results.getResultVars().forEach(var -> {
				if (binding.contains(var)) {
					if (binding.get(var).isLiteral()) {
						map.put(var, binding.get(var).asLiteral().getString());
					} else if (binding.get(var).isResource()) {
						map.put(var, "<" + binding.get(var).asResource().toString() + ">");
					} else {
						map.put(var, binding.get(var).toString());
					}
				}
			});
		});
		template.process(Collections.singletonMap("data", data), new FileWriter(outputFile, StandardCharsets.UTF_8));
	}

	public void executePlan(String planIri)
			throws IllegalArgumentException, ClassCastException, ReflectiveOperationException {
		// get configuration model
		Model configurationModel = datasetForExecution.getDefaultModel();

		// get plan to process
		Resource plan = Plans.getPlan(configurationModel, planIri);

		// get aspects
		Aspect[] aspects = Aspect.getAspects(configurationModel).toArray(Aspect[]::new);

		// write aspect variable paths into configuration model
		for (Aspect aspect : aspects) {
			aspect.determineVarPaths(configurationModel);
		}

		// get steps and predecessors
		Map<Resource, Set<Resource>> predecessors = Plans.getStepPredecessors(configurationModel, plan);

		// get execution order
		List<Resource> stepOrder = new ArrayList<>(predecessors.keySet());
		// sort by number of (transitive) dependencies to ensure
		stepOrder.sort(Comparator.comparingInt(x -> predecessors.get(x).size()));

		// setup and run pipeline
		Executor executor = Executors.newCachedThreadPool();
		Map<Resource, Step> steps = new HashMap<>();
		Map<Resource, CompletableFuture<?>> stepFutures = new HashMap<>();
		for (Resource stepIri : stepOrder) {
			// setup step
			Collection<Step> inputSteps = predecessors.get(stepIri).stream().map(steps::get)
					.collect(Collectors.toList());
			Step step = new Step(relativePathBase, datasetForExecution, configurationModel, stepIri, inputSteps, aspects);
			steps.put(stepIri, step);
			// schedule step
			CompletableFuture<?>[] inputFutures = predecessors.get(stepIri).stream().map(stepFutures::get)
					.toArray(i -> new CompletableFuture<?>[i]);
			CompletableFuture<?> stepFuture = CompletableFuture.allOf(inputFutures).thenRunAsync(step, executor);
			stepFutures.put(stepIri, stepFuture);
		}
		// expect completion of all steps
		CompletableFuture.allOf(stepFutures.values().toArray(new CompletableFuture[0])).join();
	}

	public static void initApacheJena() {
		JenaSystem.init();

		// TODO use caching HTTP client
		// HttpEnv.setDftHttpClient(dftHttpClient);
	}

	static class ManifestVersionProvider implements IVersionProvider {
		public String[] getVersion() throws Exception {
			Enumeration<URL> resources = ManifestVersionProvider.class.getClassLoader()
					.getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				Manifest manifest = new Manifest(resources.nextElement().openStream());
				String title = (String) manifest.getMainAttributes().get(new Attributes.Name("Implementation-Title"));
				if ("ABECTO".equals(title)) {
					String version = (String) manifest.getMainAttributes()
							.get(new Attributes.Name("Implementation-Version"));
					return new String[] { title + " " + version };
				}
			}
			return new String[0];
		}
	}
}