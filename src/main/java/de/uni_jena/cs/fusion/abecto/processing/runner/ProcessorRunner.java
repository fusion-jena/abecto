package de.uni_jena.cs.fusion.abecto.processing.runner;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.processor.transformation.TransformationProcessor;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;

@Component
public class ProcessorRunner {

	@Autowired
	ProcessingParameterRepository processingParameters;
	@Autowired
	ProcessingRepository processings;

	public Processing executeProcessingConfiguration(ProcessingConfiguration configuration, ProgressListener listener) {
		Collection<Processing> inputProcessings = new HashSet<>();
		for (ProcessingConfiguration inputProcessingConfiguration : configuration.getInputProcessingConfigurations()) {
			inputProcessings.add(processings.findTopByConfigurationOrderByStartDateTimeDesc(inputProcessingConfiguration));
		}

		return this.executeProcessingConfiguration(configuration, listener, inputProcessings);
	}

	public Processing executeProcessingConfiguration(ProcessingConfiguration configuration, ProgressListener listener,
			Collection<Processing> inputProcessings) {
		Processing processing = new Processing();
		try {
			// get processor
			Processor processor = configuration.getProcessor();
			processor.setListener(listener);

			// set processor parameters
			processing.setParameter(configuration.getParameter());
			processor.setProperties(configuration.getParameter().getAll());
			Collection<RdfGraph> inputGraphs = new HashSet<>();
			for (Processing inputProcessing : inputProcessings) {
				inputGraphs.add(inputProcessing.getRdfGraph());
			}
			if (processor instanceof TransformationProcessor) {
				((TransformationProcessor) processor).setInputGraphs(inputGraphs);
			}

			// set start date time
			processing.setStartTime(LocalDateTime.now());

			processing.setRdfGraph(processor.call());

		} catch (Throwable t) {
			// set StackTrace
			processing.setStackTrace(t);
		}
		// set end date time
		processing.setEndTime(LocalDateTime.now());

		return processings.save(processing);
	}
}
