package de.uni_jena.cs.fusion.abecto.runner;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModel;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModelRepository;

@Component
public class ProcessorRunner {

	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	RdfModelRepository rdfModelRepository;

	@Async
	public void execute(Processing processing, Processor<?> processor) {
		Logger log = LoggerFactory.getLogger(this.getClass());
		try {
			log.info("Running processor " + processor);
			this.processingRepository.save(processing.setStateStart());
			Model model = processor.call();
			RdfModel rdfModel = this.rdfModelRepository.save(new RdfModel(model));
			this.processingRepository.save(processing.setStateSuccess(rdfModel));
			log.info("Processor " + processor + " succeded");
		} catch (Throwable e) {
			log.error("Processor " + processor + " failed", e);
			processor.fail();
			this.processingRepository.save(processing.setStateFail(e));
		}

	}
}
