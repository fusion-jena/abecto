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
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.datatype.Sparql11SelectQueryType;
import de.uni_jena.cs.fusion.abecto.datatype.XsdDateTimeStampType;
import de.uni_jena.cs.fusion.abecto.util.Datasets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(description = "Compares and evaluates several RDF datasets.", name = "abecto", mixinStandardHelpOptions = true, versionProvider = Abecto.ManifestVersionProvider.class)
public class Abecto implements Callable<Integer> {

	final static Logger log = LoggerFactory.getLogger(Abecto.class);

	public static void main(String... args) throws Exception {
		int exitCode = new CommandLine(new Abecto()).execute(args);
		System.exit(exitCode);
	}

	@Option(names = { "-p",
			"--plan" }, paramLabel = "plan-iri", description = "IRI of the plan to process. Required, if the configuration contains multiple plans.")
	String planIri;

	@Parameters(index = "0", paramLabel = "configuration-file", description = "RDF dataset file containing the execution plan configuration.")
	File configurationFile;

	@Parameters(index = "1", paramLabel = "result-file", description = "RDF dataset file for the execution results.")
	File outputFile;

	@Override
	public Integer call() {
		try {
			executePlan(configurationFile, planIri, outputFile);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return 2;
		}

		return 0;
	}

	public static void executePlan(File configurationFile, String planIri, File outputFile) throws Throwable {
		initApacheJena();

		// derive relative base path
		File relativeBasePath = configurationFile.getParentFile();

		// read configuration
		Dataset dataset = DatasetFactory.createGeneral();

		Datasets.read(dataset, new FileInputStream(configurationFile));

		// get configuration model
		Model configurationModel = dataset.getDefaultModel();

		// get plan to process
		Resource plan = Plans.getPlan(configurationModel, planIri);

		// get aspects
		Aspect[] aspects = Aspect.getAspects(configurationModel).toArray(l -> new Aspect[l]);

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

		// write results
//		outputFile.createNewFile();
		RDFDataMgr.write(new FileOutputStream(outputFile), dataset, Lang.TRIG);
	}

	public static void initApacheJena() {
		JenaSystem.init();
		// register custom datatypes
		TypeMapper.getInstance().registerDatatype(new XsdDateTimeStampType());
		TypeMapper.getInstance().registerDatatype(new Sparql11SelectQueryType());
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