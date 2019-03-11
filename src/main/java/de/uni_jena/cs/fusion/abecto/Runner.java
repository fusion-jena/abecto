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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.runner.ProcessorRunner;
import de.uni_jena.cs.fusion.abecto.processor.progress.NullProgressListener;
import de.uni_jena.cs.fusion.abecto.processor.source.PathSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.transformation.OpenlletReasoningProcessor;
import de.uni_jena.cs.fusion.abecto.processor.transformation.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.processor.transformation.TransformationProcessor;
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
	ProcessorRunner runner;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		// create a Project
		Project project = new Project("a");
		project = projects.save(project);

		// create a KnowledgeBase
		KnowledgeBase knowledgeBase = new KnowledgeBase(project, "b");
		knowledgeBase = knowledgeBases.save(knowledgeBase);

		// create a KnowledgeBaseModule
		KnowledgeBaseModule knowledgeBaseModule = new KnowledgeBaseModule(knowledgeBase, "c");
		knowledgeBaseModule = knowledgeBaseModules.save(knowledgeBaseModule);

		knowledgeBaseModules.save(new KnowledgeBaseModule(
				knowledgeBases.save(new KnowledgeBase(projects.save(new Project("d")), "e")), "f"));

		for (KnowledgeBaseModule x : knowledgeBaseModules.findAll()) {
			log.info(x.toString());
		}

		// create a ProcessingParameter instance
		ProcessingParameter parameter = new ProcessingParameter();
		parameter.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl");

		// create a ProcessingConfiguration
		ProcessingConfiguration configuration = new ProcessingConfiguration(parameter, PathSourceProcessor.class,
				knowledgeBaseModule);

		// execute configuration
		Processing processing = runner.executeProcessingConfiguration(configuration, NullProgressListener.get());
		RdfGraph sourceGraph = processing.getRdfGraph();

		// save some RdfGraphs
//		SourceProcessor source = new PathSourceProcessor();
//		source.setProperties(Collections.singletonMap("path",
//				"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl"));
//		RdfGraph sourceGraph = source.call();
//		sourceGraph = rdfGraphs.save(sourceGraph);

		TransformationProcessor construct = new SparqlConstructProcessor();
		construct.setProperties(Collections.singletonMap("query",
				"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}"));
		construct.setInputGraphs(Collections.singleton(sourceGraph));
		RdfGraph constructedGraph = construct.call();
		constructedGraph = rdfGraphs.save(constructedGraph);

		TransformationProcessor reasoner = new OpenlletReasoningProcessor();
		reasoner.setInputGraphs(Collections.singleton(sourceGraph));
		RdfGraph inferredGraph = reasoner.call();
		inferredGraph = rdfGraphs.save(inferredGraph);

		// fetch all
		log.info("Models found with findAll():");
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
