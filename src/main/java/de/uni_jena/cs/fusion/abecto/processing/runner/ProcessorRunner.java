package de.uni_jena.cs.fusion.abecto.processing.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.progress.ProgressListener;
import de.uni_jena.cs.fusion.abecto.processor.progress.SubtaskProgressManager;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraph;
import de.uni_jena.cs.fusion.abecto.rdfGraph.RdfGraphRepository;

@Component
public class ProcessorRunner {

	@Autowired
	ProcessingConfigurationRepository configurationRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	RdfGraphRepository rdfGraphs;

	@Deprecated
	public Processing execute(ProcessingConfiguration configuration, ProgressListener listener) {
		Collection<Processing> inputProcessings = new HashSet<>();
		for (ProcessingConfiguration inputProcessingConfiguration : configuration.getInputProcessingConfigurations()) {
			inputProcessings.add(
					processingRepository.findTopByConfigurationOrderByStartDateTimeDesc(inputProcessingConfiguration));
		}

		return this.execute(configuration, inputProcessings, listener);
	}

	/**
	 * Executes a given {@link ProcessingConfiguration} with given input
	 * {@link Processing}s.
	 * 
	 * @param configuration    {@link ProcessingConfiguration} to execute
	 * @param inputProcessings input {@link Processing}s to use
	 * @param listener         {@link ProgressListener} to use
	 * @return {@link Processing} representing the actual execution
	 */
	public Processing execute(ProcessingConfiguration configuration, Collection<Processing> inputProcessings,
			ProgressListener listener) {

		// initialize processing
		Processing processing = processingRepository.save(new Processing(configuration, inputProcessings));

		try {
			// execute processor
			processing = processingRepository.save(processing.setStateStart());
			Processor processor = processing.getConfiguredProcessor(listener);
			RdfGraph resultGraph = rdfGraphs.save(processor.call());
			processing = processingRepository.save(processing.setStateSuccess(resultGraph));
		} catch (Throwable t) {
			processing = processingRepository.save(processing.setStateFail(t));
		}

		return processing;
	}

	/**
	 * Executes the processing pipeline of a given {@link Project} reusing the given
	 * {@link Processing}s.
	 * 
	 * @param project            {@link Project} to execute the belonging pipeline
	 * @param processingsToReuse {@link Processing}s to reuse
	 * @param listener           {@link ProgressListener} to use
	 * @return {@link Processing}s reused or created
	 * @throws IllegalStateException    if a loop was found in the
	 *                                  {@link ProcessingConfiguration} dependencies
	 *                                  of the {@link Project}
	 * @throws IllegalArgumentException if the {@link Processing}s to reuse do not
	 *                                  match to the Project or to one another
	 */
	public Collection<Processing> execute(Project project, Collection<Processing> processingsToReuse,
			ProgressListener listener) throws IllegalArgumentException, IllegalStateException {
		// get processing configurations in execution order
		Queue<ProcessingConfiguration> executionOrder = getExecutionOrder(project);

		// map given and implied processings to processing configurations
		Map<ProcessingConfiguration, Processing> processingMapping = new HashMap<>();
		Queue<Processing> processingsQueue = new LinkedList<>(processingsToReuse);
		while (!processingsQueue.isEmpty()) {
			Processing processing = processingsQueue.remove();
			ProcessingConfiguration configuration = processing.getConfiguration();
			if (!project.equals(configuration.getProject())) {
				throw new IllegalArgumentException(String.format("%s does not belog to %s.", processing, project));
			} else if (!executionOrder.contains(configuration)) {
				throw new IllegalArgumentException(
						String.format("%s does not belog to any scheduled ProcessingConfiguration.", processing));
			} else if (processingMapping.containsKey(configuration)
					&& !processing.equals(processingMapping.get(configuration))) {
				throw new IllegalArgumentException(String.format("Multiple processings given or implied for %s: %s, %s",
						configuration, processing, processingMapping.get(configuration)));
			} else {
				processingMapping.put(configuration, processing);
				processingsQueue.addAll(processing.getInputProcessings());
			}
		}

		// TODO compute processings for not mapped processing configurations
		Collection<Processing> processings = new ArrayList<>();
		SubtaskProgressManager progress = new SubtaskProgressManager(listener, executionOrder.size());
		while (!executionOrder.isEmpty()) {
			ProcessingConfiguration configuration = executionOrder.remove();
			ProgressListener subtaskListener = progress.getSubtaskProgressListener();
			if (processingMapping.containsKey(configuration)) {
				// nothing needs to be done
				processings.add(processingMapping.get(configuration));
			} else if (false) {
				// TODO check for existing executions
			} else {
				// gather input processings
				Collection<Processing> inputProcessings = configuration.getInputProcessingConfigurations().stream()
						.map(processingMapping::get).collect(Collectors.toList());
				processingMapping.put(configuration, this.execute(configuration, inputProcessings, subtaskListener));
			}
			subtaskListener.onSuccess();
		}

		progress.onSuccess();
		
		// return all processings used or created
		return processingMapping.values();
	}

	/**
	 * Returns a execution order of {@link ProcessingConfiguration}s for a given
	 * project.
	 * 
	 * @param project {@link Project} to generate a execution plan for
	 * @return {@link Queue} of {@link ProcessingConfiguration} in execution order
	 * @throws IllegalStateException if a loop was found in the
	 *                               {@link ProcessingConfiguration} dependencies of
	 *                               the {@link Project}
	 */
	public Queue<ProcessingConfiguration> getExecutionOrder(Project project) throws IllegalStateException {

		// TODO filter disabled ProcessingConfigurations

		// get all configurations of the project
		Iterable<ProcessingConfiguration> configurations = configurationRepository.findAllByProject(project);

		// gather dependencies of all configurations
		Map<ProcessingConfiguration, Set<ProcessingConfiguration>> dependencyMap = new HashMap<>();
		for (ProcessingConfiguration configuration : configurations) {
			Set<ProcessingConfiguration> dependencies = new HashSet<>();

			Queue<ProcessingConfiguration> queue = new LinkedList<>(configuration.getInputProcessingConfigurations());

			while (!queue.isEmpty()) {
				ProcessingConfiguration dependency = queue.remove();
				// check for loops in configuration dependencies
				if (!dependencies.contains(dependency)) {
					queue.addAll(dependency.getInputProcessingConfigurations());
					dependencies.add(dependency);
				} else {
					throw new IllegalStateException(
							"Failed to generate execution plan: Dependenies of " + configuration + " contain cycle.");
				}
			}

			dependencyMap.put(configuration, dependencies);
		}

		// return execution order of configurations (ordered by number of dependencies)
		return dependencyMap.entrySet().stream().sorted((a, b) -> {
			return a.getValue().size() - b.getValue().size();
		}).map(Map.Entry::getKey).collect(LinkedList::new, List::add, (a, b) -> a.addAll(b));
	}
}
