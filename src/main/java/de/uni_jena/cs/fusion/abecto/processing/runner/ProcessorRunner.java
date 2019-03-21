package de.uni_jena.cs.fusion.abecto.processing.runner;

import org.apache.jena.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraphRepository;

@Component
public class ProcessorRunner {

	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	RdfGraphRepository rdfGraphRepository;

	@Async
	public void execute(Processing processing, Processor processor) {
		Logger log = LoggerFactory.getLogger(this.getClass());
		try {
			log.info("Running processor " + processor);
			processing.setStateStart();
			Graph graph = processor.call();
			RdfGraph rdfGraph = rdfGraphRepository.save(new RdfGraph(graph));
			processing.setStateSuccess(rdfGraph);
			log.info("Processor " + processor + " succeded");
		} catch (Throwable e) {
			log.error("Processor " + processor + " failed", e);
			processor.fail();
			processing.setStateFail(e);
		}
	}
}
