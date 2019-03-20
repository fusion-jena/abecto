package de.uni_jena.cs.fusion.abecto;

import java.util.Collections;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.runner.ProjectExecutor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.OpenlletReasoningProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.PathSourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModule;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.module.KnowledgeBaseModuleRepository;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraphRepository;

@Component
public class Runner implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	@Autowired
	ProjectRepository projects;
	@Autowired
	KnowledgeBaseRepository knowledgeBases;
	@Autowired
	KnowledgeBaseModuleRepository knowledgeBaseModules;
	@Autowired
	RdfGraphRepository rdfGraphs;
	@Autowired
	ProcessingParameterRepository processingParameters;
	@Autowired
	ProcessingConfigurationRepository configurations;
	@Autowired
	ProjectExecutor projectExecutor;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		// create a Project
		Project project = projects.save(new Project("a"));

		// create some KnowledgeBase
		KnowledgeBase knowledgeBase = knowledgeBases.save(new KnowledgeBase(project, "b"));
		knowledgeBases.save(new KnowledgeBase(project, "d"));

		// create some KnowledgeBaseModule
		KnowledgeBaseModule knowledgeBaseModule = knowledgeBaseModules
				.save(new KnowledgeBaseModule(knowledgeBase, "c"));
		knowledgeBaseModules.save(new KnowledgeBaseModule(knowledgeBase, "e"));

		for (KnowledgeBaseModule x : knowledgeBaseModules.findAll()) {
			log.info(x.toString());
		}

		// execute PathSourceProcessor
		ProcessingParameter parameterPathSource = new ProcessingParameter();
		parameterPathSource.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl");
		parameterPathSource = processingParameters.save(parameterPathSource);
		ProcessingConfiguration configurationPathSource = configurations
				.save(new ProcessingConfiguration(PathSourceProcessor.class, parameterPathSource, knowledgeBaseModule));

		// execute SparqlConstructProcessor
		ProcessingParameter parameterSparqlConstruct = new ProcessingParameter();
		parameterSparqlConstruct.set("query",
				"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}");
		parameterSparqlConstruct = processingParameters.save(parameterSparqlConstruct);
		configurations.save(new ProcessingConfiguration(SparqlConstructProcessor.class, parameterSparqlConstruct,
				Collections.singleton(configurationPathSource)));

		// execute OpenlletReasoningProcessor
		ProcessingParameter parameterOpenlletReasoning = new ProcessingParameter();
		parameterSparqlConstruct = processingParameters.save(parameterOpenlletReasoning);
		configurations.save(new ProcessingConfiguration(OpenlletReasoningProcessor.class, parameterOpenlletReasoning,
				Collections.singleton(configurationPathSource)));

		projectExecutor.execute(project);
	}

	@Scheduled(fixedDelay = 5000)
	public void reportRdfGraphs() {
		// fetch all
		log.info("RdfGraphs found with findAll():");
		log.info("-------------------------------");
		for (RdfGraph graph : rdfGraphs.findAll()) {
			log.info(graph.toString());

			log.info("Content Examples:");
			// prepare query
			Query query = QueryFactory
					.create("SELECT * WHERE {?s ?p ?o. Filter(!isBLANK(?s) && !isBLANK(?o))} LIMIT 10");
			// prepare execution
			Model model = graph.getModel();
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			// execute and process result
			ResultSet result = queryExecution.execSelect();
			while (result.hasNext()) {
				QuerySolution solution = result.next();
				log.info("  " + solution.toString());
			}
		}
	}

}
