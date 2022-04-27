/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
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
 */
package de.uni_jena.cs.fusion.abecto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.datatype.SparqlPropertyPathType;
import de.uni_jena.cs.fusion.abecto.datatype.SparqlQueryType;
import de.uni_jena.cs.fusion.abecto.datatype.XsdDateTimeStampType;
import de.uni_jena.cs.fusion.abecto.util.Datasets;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
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
		java.util.logging.Logger.getLogger("org.apache.jena.riot").setLevel(java.util.logging.Level.OFF);
		java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.ALL);

		// init Apache Jena
		initApacheJena();
	}

	final static Logger log = LoggerFactory.getLogger(Abecto.class);

	public static void main(String... args) throws Exception {
		int exitCode = new CommandLine(new Abecto()).execute(args);
		System.exit(exitCode);
	}

	@Option(names = { "-p",
			"--plan" }, paramLabel = "Plan IRI", description = "IRI of the plan to process. Required, if the configuration contains multiple plans.")
	String planIri;

	@Option(names = "--trig", paramLabel = "TRIG Output File", description = "RDF TRIG dataset file for the execution results.")
	File trigOutputFile;

	@Option(names = "--loadOnly", paramLabel = "Load Only", description = "If set, the plan will not get executed. This enables to export results without repeated plan execution.")
	boolean loadOnly;

	@Option(names = { "-E",
			"--export" }, paramLabel = "Export Template and File", description = "Template and output file for an result export. Can be set multiple times.")
	Map<String, File> exports;

	@Parameters(index = "0", paramLabel = "Plan Dataset File", description = "RDF dataset file containing the plan configuration and optionally plan execution results (see --loadOnly).")
	File planDatasetFile;

	private Dataset dataset;
	private File relativeBasePath;
	private Configuration freemarker;
	private final static String TEMPLATE_FOLDER = "/de/uni_jena/cs/fusion/abecto/export";

	@Override
	public Integer call() {
		try {
			log.info("Loading plan dataset file started.");
			loadDataset(planDatasetFile);
			log.info("Loading plan dataset file completed.");

			if (!loadOnly) {
				log.info("Plan execution started.");
				executePlan(planIri);
				log.info("Plan execution completed.");
			} else {
				log.info("Plan execution skipped.");
			}

			reuseModelNamespaces(dataset);
			
			// write results as TRIG
			if (trigOutputFile != null && !loadOnly) {
				log.info("Writing plan execution results as TRIG file started.");
				RDFDataMgr.write(new FileOutputStream(trigOutputFile), dataset, Lang.TRIG);
				log.info("Writing plan execution results as TRIG file completed.");
			}

			// export results
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
					export(export.getKey(), export.getValue());
					log.info(String.format("Export with template \"%s\" completed.", export.getKey()));
				}
			}
		} catch (CompletionException e) {
			log.error("Plan execution failed.", e.getCause());
			return 2;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return 2;
		}

		return 0;
	}

	public void reuseModelNamespaces(Dataset dataset) {
		PrefixMapping datasetPrefixMapping = dataset.getPrefixMapping();
		datasetPrefixMapping.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
		dataset.listModelNames().forEachRemaining(
				modelName -> datasetPrefixMapping.setNsPrefixes(dataset.getNamedModel(modelName).getNsPrefixMap()));
	}

	public void loadDataset(File configurationFile)
			throws IllegalArgumentException, FileNotFoundException, IOException {

		// derive relative base path
		this.relativeBasePath = configurationFile.getParentFile();

		// read configuration
		this.dataset = DatasetFactory.createGeneral();

		Datasets.read(this.dataset, new FileInputStream(configurationFile));
	}

	public void export(String exportType, File outputFile) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Template template = freemarker.getTemplate(exportType + ".ftlh");
		String queryStr = new String(
				this.getClass().getResourceAsStream(TEMPLATE_FOLDER + "/" + exportType + ".rq").readAllBytes(),
				StandardCharsets.UTF_8);
		List<Map<String, String>> data = new ArrayList<>();
		ResultSet results = QueryExecutionFactory.create(queryStr, dataset).execSelect();
		results.forEachRemaining(binding -> {
			Map<String, String> map = new HashMap<>();
			data.add(map);
			results.getResultVars().forEach(var -> {
				if (binding.contains(var)) {
					map.put(var, binding.get(var).toString());
				}
			});
		});
		template.process(Collections.singletonMap("data", data), new FileWriter(outputFile, StandardCharsets.UTF_8));
	}

	public void executePlan(String planIri)
			throws IllegalArgumentException, ClassCastException, ReflectiveOperationException {
		// get configuration model
		Model configurationModel = dataset.getDefaultModel();

		// get plan to process
		Resource plan = Plans.getPlan(configurationModel, planIri);

		// get aspects
		Aspect[] aspects = Aspect.getAspects(configurationModel).toArray(l -> new Aspect[l]);

		// write aspect variable paths into configuration model
		for (Aspect aspect : aspects) {
			aspect.determineVarPaths(configurationModel);
		}

		// get steps and predecessors
		Map<Resource, Set<Resource>> predecessors = Plans.getStepPredecessors(configurationModel, plan);

		// get execution order
		List<Resource> stepOrder = new ArrayList<>(predecessors.keySet());
		// sort by number of (transitive) dependencies to ensure
		Collections.sort(stepOrder, (x, y) -> Integer.compare(predecessors.get(x).size(), predecessors.get(y).size()));

		// setup and run pipeline
		Executor executor = Executors.newCachedThreadPool();
		Map<Resource, Step> steps = new HashMap<>();
		Map<Resource, CompletableFuture<?>> stepFutures = new HashMap<>();
		for (Resource stepIri : stepOrder) {
			// setup step
			Collection<Step> inputSteps = predecessors.get(stepIri).stream().map(steps::get)
					.collect(Collectors.toList());
			Step step = new Step(relativeBasePath, dataset, configurationModel, stepIri, inputSteps, aspects);
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
		// register custom datatypes
		TypeMapper.getInstance().registerDatatype(new XsdDateTimeStampType());
		TypeMapper.getInstance().registerDatatype(new SparqlQueryType());
		TypeMapper.getInstance().registerDatatype(new SparqlPropertyPathType());

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