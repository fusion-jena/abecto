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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import de.uni_jena.cs.fusion.abecto.processor.PathSource;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SubsequentProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfModelRepository;

@SpringBootApplication
public class Abecto {

	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	public static void main(String[] args) {
		SpringApplication.run(Abecto.class, args);
	}

	/*
	 * @Bean public CommandLineRunner demo(ProjectRepository repository) { return
	 * (args) -> { // save a couple repository.save(new Project("Bauer"));
	 * repository.save(new Project("O'Brian")); repository.save(new
	 * Project("Bauer")); repository.save(new Project("Palmer"));
	 * repository.save(new Project("Dessler"));
	 * 
	 * // fetch all log.info("Projects found with findAll():");
	 * log.info("-------------------------------"); for (Project customer :
	 * repository.findAll()) { log.info(customer.toString()); } log.info("");
	 * 
	 * // fetch an individual by ID repository.findById(1L).ifPresent(project -> {
	 * log.info("Project found with findById(1L):");
	 * log.info("--------------------------------"); log.info(project.toString());
	 * log.info(""); });
	 * 
	 * // fetch by name log.info("Project found with findByLastName('Bauer'):");
	 * log.info("--------------------------------------------");
	 * repository.findByName("Bauer").forEach(bauer -> { log.info(bauer.toString());
	 * }); // for (Project bauer : repository.findByLastName("Bauer")) { //
	 * log.info(bauer.toString()); // } log.info(""); }; }
	 */
	@Bean
	public CommandLineRunner modelDemo(RdfModelRepository repository) {
		return (args) -> {
			// save a couple
			SourceProcessor source = new PathSource();
			source.setProperties(Collections.singletonMap("path",
					"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl"));
			RdfGraph sourceModel = source.call();
			sourceModel = repository.save(sourceModel);

			SubsequentProcessor construct = new SparqlConstructProcessor();
			construct.setProperties(Collections.singletonMap("query",
					"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}"));
			construct.setSources(Collections.singleton(sourceModel));
			RdfGraph constructModel = construct.call();
			constructModel = repository.save(constructModel);

			// fetch all
			log.info("Models found with findAll():");
			log.info("-------------------------------");
			for (RdfGraph graph : repository.findAll()) {
				log.info(graph.toString());

				log.info("Content Examples:");
				// prepare query
				Query query = QueryFactory.create("SELECT * WHERE {?s ?p ?o. Filter(!isBLANK(?s) && !isBLANK(?o))} LIMIT 10");
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

			/*
			 * Model model = repository.findById(1L).get().getModel(); String queryString =
			 * " .... " ; Query query = QueryFactory.create(queryString) ; try
			 * (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			 * ResultSet results = qexec.execSelect() ; for ( ; results.hasNext() ; ) {
			 * QuerySolution soln = results.nextSolution() ; RDFNode x = soln.get("varName")
			 * ; // Get a result variable by name. Resource r = soln.getResource("VarR") ;
			 * // Get a result variable - must be a resource Literal l =
			 * soln.getLiteral("VarL") ; // Get a result variable - must be a literal } }
			 */
		};
	}

}