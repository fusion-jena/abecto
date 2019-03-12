package de.uni_jena.cs.fusion.abecto.processing.runner;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraphRepository;

@Component
public class ProcessorRunner {

	@Autowired
	ProcessingRepository processings;
	@Autowired
	RdfGraphRepository rdfGraphs;

	public Processing executeProcessingConfiguration(ProcessingConfiguration configuration, ProgressListener listener) {
		Collection<Processing> inputProcessings = new HashSet<>();
		for (ProcessingConfiguration inputProcessingConfiguration : configuration.getInputProcessingConfigurations()) {
			inputProcessings
					.add(processings.findTopByConfigurationOrderByStartDateTimeDesc(inputProcessingConfiguration));
		}

		return this.executeProcessingConfiguration(configuration, listener, inputProcessings);
	}

	public Processing executeProcessingConfiguration(ProcessingConfiguration configuration, ProgressListener listener,
			Collection<Processing> inputProcessings) {
		
		// initialize processing
		Processing processing = processings.save(new Processing(configuration, inputProcessings));
		
		try {
			// execute processor
			processing = processings.save(processing.setStateStart());
			Processor processor = processing.getConfiguredProcessor(listener);
			RdfGraph resultGraph = rdfGraphs.save(processor.call());
			processing = processings.save(processing.setStateSuccess(resultGraph));
		} catch (Throwable t) {
			processing = processings.save(processing.setStateFail(t));
		}

		return processing;
	}
}
