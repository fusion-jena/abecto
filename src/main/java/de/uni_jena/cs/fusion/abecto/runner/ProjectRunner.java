package de.uni_jena.cs.fusion.abecto.runner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.configuration.Configuration;
import de.uni_jena.cs.fusion.abecto.configuration.ConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingException;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.RefinementProcessor;
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
	ConfigurationRepository configurationRepository;
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
		Iterable<Configuration> configurations = this.configurationRepository.findAllByProject(project);

		Map<Configuration, Processing> processingsMap = new HashMap<>();

		try {
			log.info("add given processings and their input processings to processingsMap");
			Queue<Processing> completedProcessings = new LinkedList<>(processings);
			while (!completedProcessings.isEmpty()) {
				Processing completedProcessing = completedProcessings.poll();
				processingsMap.put(completedProcessing.getConfiguration(), completedProcessing);
				completedProcessings.addAll(completedProcessing.getInputProcessings());
			}

			log.info("add new processings for missing configurations to processingsMap");
			for (Configuration configuration : configurations) {
				processingsMap.computeIfAbsent(configuration, (c) -> new Processing(c));
			}

			// save processings
			this.processingRepository.saveAll(processingsMap.values());

			Map<Configuration, Processor<?>> processorsMap = new HashMap<>();

			log.info("initialize processors for not startet processings");
			// initialize processors for not started processings
			for (Configuration configuration : configurations) {
				Processing processing = processingsMap.get(configuration);
				try {
					Processor<?> processor = processing.getProcessorInsance();
					processor.setParameters(configuration.getParameter().getParameters());
					processorsMap.put(configuration, processor);
				} catch (Throwable t) {
					if (processing.isNotStarted()) {
						this.processingRepository.save(processing.setStateFail(t));
					}
				}

			}

			log.info("add dependent processors or input graphs to processors");
			// add dependent processors
			for (Configuration configuration : processorsMap.keySet()) {
				Processor<?> processor = processorsMap.get(configuration);
				for (Configuration inputConfiguration : configuration.getInputProcessingConfigurations()) {
					if (processor instanceof RefinementProcessor) {
						((RefinementProcessor<?>) processor).addInputProcessor(processorsMap.get(inputConfiguration));
					}
				}
			}

			// execute processors
			for (Configuration configuration : processorsMap.keySet()) {
				log.info("Scheduling processor: " + processorsMap.get(configuration));
				processorRunner.execute(processingsMap.get(configuration), processorsMap.get(configuration));
			}

		} catch (Throwable t) {
			log.error("Error:", t);
			for (Processing processing : processingsMap.values()) {
				if (processing.isNotStarted() || processing.isRunning()) {
					processing.setStateFail(new ProcessingException("Pipeline execution failed.", t));
					this.processingRepository.save(processing);
				}
			}
		}
	}
}
