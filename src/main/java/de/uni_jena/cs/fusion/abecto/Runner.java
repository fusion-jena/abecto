package de.uni_jena.cs.fusion.abecto;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.runner.ProjectRunner;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.ManualRelationSelectionProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping.JaroWinklerMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.OpenlletReasoningProcessor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.transformation.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.PathSourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModel;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModelRepository;

//@Component
public class Runner implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProcessingRepository processingRepository;
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
	@Autowired
	ProcessingParameterRepository processingParameterRepository;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		// create a Project
		Project projectUnits = projectRepository.save(new Project("unit ontologies"));

		// create KnowledgeBases
		KnowledgeBase knowledgeBaseQU = knowledgeBaseRepository.save(new KnowledgeBase(projectUnits, "QU"));
		KnowledgeBase knowledgeBaseOM = knowledgeBaseRepository.save(new KnowledgeBase(projectUnits, "OM"));

		// create ProcessingConfigurations
		ProcessingConfiguration configurationQU1 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu.owl")),
						knowledgeBaseQU));

		ProcessingConfiguration configurationQU2 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl")),
						knowledgeBaseQU));

		ProcessingConfiguration configurationMUO1 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\muo\\muo-vocab.owl")),
						knowledgeBaseOM));

		ProcessingConfiguration configurationMUO2 = processingConfigurationRepository
				.save(new ProcessingConfiguration(PathSourceProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\muo\\ucum-instances.owl")),
						knowledgeBaseOM));

		ProcessingConfiguration configurationQUCategories = processingConfigurationRepository
				.save(new ProcessingConfiguration(ManualRelationSelectionProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("relations",
								Map.of("instance", "rdfs:type", "label", "rdfs:label"))),
						List.of(configurationQU1, configurationQU2)));

		ProcessingConfiguration configurationMUOCategories = processingConfigurationRepository
				.save(new ProcessingConfiguration(ManualRelationSelectionProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("relations",
								Map.of("instance", "rdfs:type", "label", "rdfs:label"))),
						List.of(configurationMUO1, configurationMUO2)));

		ProcessingConfiguration configurationJWSMapper = processingConfigurationRepository
				.save(new ProcessingConfiguration(JaroWinklerMappingProcessor.class,
						this.processingParameters
								.save(new ProcessingParameter().set("case_sensitive", false).set("threshold", 0.95D)),
						List.of(configurationQU1, configurationQU2, configurationQUCategories, configurationMUO1,
								configurationMUO2, configurationMUOCategories)));

		ProcessingConfiguration configurationSparqlConstruct = processingConfigurationRepository
				.save(new ProcessingConfiguration(SparqlConstructProcessor.class,
						this.processingParameters.save(new ProcessingParameter().set("query",
								"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}")),
						Collections.singleton(configurationQU2)));

		ProcessingConfiguration configurationOpenlletReasoning = processingConfigurationRepository
				.save(new ProcessingConfiguration(OpenlletReasoningProcessor.class,
						this.processingParameters.save(new ProcessingParameter()),
						Collections.singleton(configurationQU2)));

		projectRunner.execute(projectUnits);
	}

	@Scheduled(fixedDelay = 10000)
	public void reportEntities() {
		log.info("Projects:");
		for (Project project : projectRepository.findAll()) {
			log.info("  " + project);
		}
		log.info("KnowledgeBases:");
		for (KnowledgeBase knowledgeBase : knowledgeBaseRepository.findAll()) {
			log.info("  " + knowledgeBase);
		}
		log.info("ProcessingConfigurations:");
		for (ProcessingConfiguration processingConfiguration : processingConfigurationRepository.findAll()) {
			log.info("  " + processingConfiguration);
		}
		log.info("ProcessingParameter:");
		for (ProcessingParameter processingParameter : processingParameterRepository.findAll()) {
			log.info("  " + processingParameter);
		}
		log.info("Processings:");
		for (Processing processing : processingRepository.findAll()) {
			log.info("  " + processing);
		}
		log.info("RdfModels:");
		for (RdfModel rdfModel : rdfModelRepository.findAll()) {
			log.info("  " + rdfModel + " - content examples:");
			Iterator<Statement> statements = rdfModel.getModel().listStatements();
			for (int i = 0; i < 10 && statements.hasNext(); i++) {
				Statement statement = statements.next();
				log.info("    " + statement.toString());
			}
		}
	}

}
