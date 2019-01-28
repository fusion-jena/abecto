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

import de.uni_jena.cs.fusion.abecto.processor.OpenlletReasoningProcessor;
import de.uni_jena.cs.fusion.abecto.processor.PathSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SparqlConstructProcessor;
import de.uni_jena.cs.fusion.abecto.processor.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraphRepository;

@SpringBootApplication
public class Abecto {

	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	public static void main(String[] args) {
		SpringApplication.exit(SpringApplication.run(Abecto.class, args));
	}

	@Bean
	public CommandLineRunner modelDemo(RdfGraphRepository repository) {
		return (args) -> {
			// save a couple
			SourceProcessor source = new PathSourceProcessor();
			source.setProperties(Collections.singletonMap("path",
					"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu-rec20.owl"));
			RdfGraph sourceModel = source.call();
			sourceModel = repository.save(sourceModel);

			TransformationProcessor construct = new SparqlConstructProcessor();
			construct.setProperties(Collections.singletonMap("query",
					"CONSTRUCT {?s <http://example.org/p> <http://example.org/o>} WHERE {?s ?p ?o. Filter(!isBLANK(?s))}"));
			construct.setSources(Collections.singleton(sourceModel));
			RdfGraph constructModel = construct.call();
			constructModel = repository.save(constructModel);

			TransformationProcessor reasoner = new OpenlletReasoningProcessor();
			reasoner.setSources(Collections.singleton(sourceModel));
			RdfGraph inferenceModel = reasoner.call();
			constructModel = repository.save(inferenceModel);

			// fetch all
			log.info("Models found with findAll():");
			log.info("-------------------------------");
			for (RdfGraph graph : repository.findAll()) {
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
		};
	}

}