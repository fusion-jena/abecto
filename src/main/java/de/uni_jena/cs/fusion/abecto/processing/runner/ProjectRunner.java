package de.uni_jena.cs.fusion.abecto.processing.runner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingException;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.refinement.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.rdfModel.RdfModelRepository;

@Component
public class ProjectRunner {
	private static final Logger log = LoggerFactory.getLogger(ProjectRunner.class);
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	RdfModelRepository rdfGraphRepository;
	@Autowired
	ProcessingConfigurationRepository configurationRepository;
	@Autowired
	ProcessorRunner processorRunner;

	/**
	 * Executes the processing pipeline of a given {@link Project}.
	 * 
	 * @param project {@link Project} to execute the belonging pipeline
	 */
	public void execute(Project project) {
		this.execute(project, Collections.emptyList());
	}

	/**
	 * Executes the processing pipeline of a given {@link Project} starting at the
	 * given {@link Processing}s. The given {@link Processing}s and their dependent
	 * {@link Processing}s will not be executed.
	 * 
	 * @param project     {@link Project} to execute the belonging pipeline
	 * @param processings {@link Processing}s to start at
	 */
	public void execute(Project project, Collection<Processing> processings) {
		log.info("execute " + project + " using " + processings);

		log.info("get all configurations of the project");
		// get all configurations of the project
		Iterable<ProcessingConfiguration> configurations = this.configurationRepository.findAllByProject(project);

		Map<ProcessingConfiguration, Processing> processingsMap = new HashMap<>();

		try {
			log.info("add given processings to processingsMap");
			// add given processings to processingsMap
			for (Processing processing : processings) {
				processingsMap.put(processing.getConfiguration(), processing);
			}

			// TODO add dependet processings of given processings to processingsMap

			log.info("add new processings for missing configurations to processingsMap");
			// add new processings for missing configurations to processingsMap
			for (ProcessingConfiguration configuration : configurations) {
				processingsMap.computeIfAbsent(configuration, (c) -> new Processing(c));
			}

			// save processings
			processingRepository.saveAll(processingsMap.values());

			Map<ProcessingConfiguration, Processor> processorsMap = new HashMap<>();

			log.info("initialize processors for not startet processings");
			// initialize processors for not startet processings
			for (ProcessingConfiguration configuration : configurations) {
				Processing processing = processingsMap.get(configuration);
				if (processing.isNotStarted()) {
					try {
						Processor processor;
						processor = processing.getProcessorInsance();
						processor.setProperties(configuration.getParameter().getAll());
						processorsMap.put(configuration, processor);
					} catch (Throwable t) {
						processing.setStateFail(t);
					}
				}
			}

			log.info("add dependent processors or input graphs to processors");
			// add dependent processors or input graphs to processors
			for (ProcessingConfiguration configuration : processorsMap.keySet()) {
				Processor processor = processorsMap.get(configuration);
				for (ProcessingConfiguration dependentConfiguration : configuration
						.getInputProcessingConfigurations()) {
					Processing dependentProcessing = processingsMap.get(dependentConfiguration);
					switch (processingsMap.get(dependentConfiguration).getStatus()) {
					case RUNNING:
						throw new UnsupportedOperationException("Dependent Processing is currently running.");
					case FAILED:
						throw new IllegalStateException("Dependent Processing is failed.");
					case SUCCEEDED:
						if (processor instanceof RefinementProcessor) {
							((RefinementProcessor) processor).addInputModelGroups(dependentProcessing.getDataModels());
							((RefinementProcessor) processor).addMetaModels(dependentProcessing.getMetaModels());
						}
						break;
					case NOT_STARTED:
						if (processor instanceof RefinementProcessor) {
							((RefinementProcessor) processor)
									.addInputProcessor(processorsMap.get(dependentConfiguration));
						}
						break;
					}
				}

			}

			// execute processors
			for (ProcessingConfiguration configuration : processorsMap.keySet()) {
				log.info("Scheduling processor: " + processorsMap.get(configuration));
				processorRunner.execute(processingsMap.get(configuration), processorsMap.get(configuration));
			}

		} catch (Throwable t) {
			log.error("Error:", t);
			for (Processing processing : processingsMap.values()) {
				if (processing.isNotStarted() || processing.isRunning()) {
					processing.setStateFail(new ProcessingException("Pipeline execution failed.", t));
					processingRepository.save(processing);
				}
			}
		}
	}
}
