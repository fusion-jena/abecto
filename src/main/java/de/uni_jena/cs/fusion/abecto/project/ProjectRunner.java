package de.uni_jena.cs.fusion.abecto.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.execution.Execution;
import de.uni_jena.cs.fusion.abecto.execution.ExecutionRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRunner;
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;

@Component
public class ProjectRunner {
	private static final Logger log = LoggerFactory.getLogger(ProjectRunner.class);
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ExecutionRepository executionRepository;
	@Autowired
	ProcessingRunner processingRunner;

	/**
	 * Schedules a given {@link Project} starting at the given {@link Processing}s.
	 * The given {@link Processing}s and their input {@link Processing}s will not be
	 * executed.
	 * 
	 * @param project     {@link Project} to execute
	 * @param processings {@link Processing}s after that to start
	 * @throws InterruptedException if the current thread was interrupted while
	 *                              waiting for the {@link Processing}s termination
	 */
	public Execution execute(UUID projectId, Collection<UUID> inputProcessingIds, boolean await)
			throws InterruptedException {
		// get project
		Project project = projectRepository.findById(projectId).orElseThrow();
		// get steps
		Iterable<Step> steps = stepRepository.findAllByProject(project);

		// initialize processing map
		Map<Step, Processing> processingsByStep = new HashMap<>();

		// get input processings
		List<Processing> inputProcessings = new LinkedList<>();
		processingRepository.findAllById(inputProcessingIds).forEach(inputProcessings::add);
		// get transitive input processings
		for (ListIterator<Processing> inputProcessingsIterator = inputProcessings
				.listIterator(); inputProcessingsIterator.hasNext();) {
			Processing inputProcessing = inputProcessingsIterator.next();
			// get transitive input processings
			inputProcessing.getInputProcessings().forEach(inputProcessingsIterator::add);
			// add input processing to map
			processingsByStep.put(inputProcessing.getStep(), inputProcessing);
		}

		// TODO avoid re-execution of steps with same parameters and input

		// create processing for left steps
		for (Step step : steps) {
			processingsByStep.computeIfAbsent(step, (c) -> new Processing(c));
		}

		// initialize result processing collection
		Collection<Processing> processingsToReturn = new ArrayList<>();


		// interlink processings
		for (Step step : steps) {
			Processing processing = processingsByStep.get(step);
			if (processing.isNotStarted()) {
				for (Step inputStep : step.getInputSteps()) {
					processing.addInputProcessing(processingsByStep.get(inputStep));
				}
			} else {
				// add processing to result
				processingsToReturn.add(processing);
			}
		}

		// save processings and execution
		Iterable<Processing> processingsToExecute = processingRepository.saveAll(processingsByStep.values());
		Execution execution = executionRepository.save(new Execution(project, processingsToExecute));

		Map<Processing, Future<Processing>> futureProcessings = new HashMap<>();

		// execute processors
		for (Processing processingToExecute : processingsToExecute) {
			if (processingToExecute.isNotStarted()) {
				try {
					futureProcessings.put(processingToExecute,
							processingRunner.asyncExecute(processingToExecute.getId()));
				} catch (IllegalStateException | NoSuchElementException e) {
					log.error(String.format("Failed to execute processing %s.", processingToExecute.getId()), e);
				}
			}
			if (!await) {
				processingsToReturn.add(processingToExecute);
			}
		}

		// await processing termination
		if (await) {
			for (Entry<Processing, Future<Processing>> futureProcessing : futureProcessings.entrySet()) {
				try {
					processingsToReturn.add(futureProcessing.getValue().get());
				} catch (ExecutionException e) {
					processingsToReturn.add(futureProcessing.getKey().setStateFail(e));
				}
			}
		}

		return execution;
	}
}
