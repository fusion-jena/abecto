package de.uni_jena.cs.fusion.abecto;

import java.util.List;

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
import de.uni_jena.cs.fusion.abecto.processing.runner.ProjectRunner;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping.JaroWinklerMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.PathSourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModel;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModelRepository;

@Component
public class Runner implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	@Autowired
	ProjectRepository projects;
	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	RdfModelRepository rdfModelRepository;
	@Autowired
	ProcessingParameterRepository processingParameters;
	@Autowired
	ProcessingConfigurationRepository processingConfigurationRepository;
	@Autowired
	ProjectRunner projectRunner;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		// create a Project
		Project projectUnits = projects.save(new Project("unit ontologies"));

		// create KnowledgeBases
		KnowledgeBase knowledgeBaseQU = knowledgeBaseRepository.save(new KnowledgeBase(projectUnits, "QU"));
		KnowledgeBase knowledgeBaseOM = knowledgeBaseRepository.save(new KnowledgeBase(projectUnits, "OM"));

		// create ProcessingConfigurations
		ProcessingParameter parameterQU1 = new ProcessingParameter();
		parameterQU1.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu.owl");
		parameterQU1 = processingParameters.save(parameterQU1);
		ProcessingConfiguration configurationQU1 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class, parameterQU1, knowledgeBaseQU));

		ProcessingParameter parameterQU2 = new ProcessingParameter();
		parameterQU2.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl");
		parameterQU2 = processingParameters.save(parameterQU2);
		ProcessingConfiguration configurationQU2 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class, parameterQU2, knowledgeBaseQU));

		ProcessingParameter parameterMUO1 = new ProcessingParameter();
		parameterMUO1.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\muo\\muo-vocab.owl");
		parameterMUO1 = processingParameters.save(parameterMUO1);
		ProcessingConfiguration configurationMUO1 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class, parameterMUO1, knowledgeBaseOM));

		ProcessingParameter parameterMUO2 = new ProcessingParameter();
		parameterMUO2.set("path", "C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\muo\\ucum-instances.owl");
		parameterMUO2 = processingParameters.save(parameterMUO2);
		ProcessingConfiguration configurationMUO2 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class, parameterMUO2, knowledgeBaseOM));

		ProcessingParameter parameterJWSMapper = processingParameters
				.save(new ProcessingParameter().set("case_sensitive", false).set("threshold", 0.95D));
		ProcessingConfiguration configurationJWSMapper = processingConfigurationRepository
				.save(new ProcessingConfiguration(JaroWinklerMappingProcessor.class, parameterJWSMapper,
						List.of(configurationQU1, configurationQU2, configurationMUO1, configurationMUO2)));

//		// execute SparqlConstructProcessor
//		ProcessingParameter parameterSparqlConstruct = new ProcessingParameter();
//		parameterSparqlConstruct.set("query",
//				"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}");
//		parameterSparqlConstruct = processingParameters.save(parameterSparqlConstruct);
//		processingConfigurationRepository.save(new ProcessingConfiguration(SparqlConstructProcessor.class,
//				parameterSparqlConstruct, Collections.singleton(configurationQU2)));

//		// execute OpenlletReasoningProcessor
//		ProcessingParameter parameterOpenlletReasoning = new ProcessingParameter();
//		parameterSparqlConstruct = processingParameters.save(parameterOpenlletReasoning);
//		processingConfigurationRepository.save(new ProcessingConfiguration(OpenlletReasoningProcessor.class,
//				parameterOpenlletReasoning, Collections.singleton(configurationQU2)));

		projectRunner.execute(projectUnits);
	}

	@Scheduled(fixedDelay = 10000)
	public void reportRdfGraphs() {
		// fetch all
		log.info("RdfGraphs found with findAll():");
		log.info("-------------------------------");
		for (RdfModel rdfModel : rdfModelRepository.findAll()) {
			log.info(rdfModel.toString());

			log.info("Content Examples:");
			// prepare query
			Query query = QueryFactory
					.create("SELECT * WHERE {?s ?p ?o. Filter(!isBLANK(?s) && !isBLANK(?o))} LIMIT 10");
			// prepare execution
			Model model = rdfModel.getModel();
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
