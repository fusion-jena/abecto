package de.uni_jena.cs.fusion.abecto.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRunner;
import de.uni_jena.cs.fusion.abecto.processing.ProcessorInstantiationException;
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
	ProcessingRunner processingRunner;

	/**
	 * Schedules a given {@link Project} starting at the given {@link Processing}s
	 * and awaits termination of each {@link Processing}. The given
	 * {@link Processing}s and their input {@link Processing}s will not be executed.
	 * 
	 * @param project     {@link Project} to execute
	 * @param processings {@link Processing}s after that to start
	 * @throws InterruptedException if the current thread was interrupted while
	 *                              waiting for the {@link Processing}s termination
	 */
	public Collection<UUID> executeAndAwait(UUID projectId, Collection<UUID> startProcessingIds)
			throws InterruptedException, ExecutionException {
		Collection<UUID> processingIds = execute(projectId, startProcessingIds);
		for (UUID processingId : processingIds) {
			try {
				this.processingRunner.await(processingId);
			} catch (NoSuchElementException | ProcessorInstantiationException e) {
				log.error("Failed to await Processing.", e);
				// each Processor has already been instantiated before, so this should not
				// happen in a consistent state
				throw new IllegalStateException("Failed to await Processing again.", e);
			}
		}
		return processingIds;
	}

	/**
	 * Schedules a given {@link Project} starting at the given {@link Processing}s.
	 * The given {@link Processing}s and their input {@link Processing}s will not be
	 * executed.
	 * 
	 * @param project     {@link Project} to execute
	 * @param processings {@link Processing}s after that to start
	 */
	public Collection<UUID> execute(UUID projectId, Collection<UUID> startProcessingIds) {
		Project project = projectRepository.findById(projectId).orElseThrow();
		Iterable<Processing> processings = processingRepository.findAllById(startProcessingIds);
		Iterable<Step> steps = stepRepository.findAllByProject(project);

		Map<Step, Processing> processingsByStep = new HashMap<>();

		// collect given processings and their input processings
		Queue<Processing> completedProcessings = new LinkedList<>();
		processings.forEach(completedProcessings::add);
		while (!completedProcessings.isEmpty()) {
			Processing completedProcessing = completedProcessings.poll();
			processingsByStep.put(completedProcessing.getStep(), completedProcessing);
			completedProcessings.addAll(completedProcessing.getInputProcessings());
		}

		// TODO avoid re-execution of steps with same parameters and input

		// create processing for left steps
		for (Step step : steps) {
			processingsByStep.computeIfAbsent(step, (c) -> new Processing(c));
		}

		// interlink new processings
		for (Step step : steps) {
			Processing processing = processingsByStep.get(step);
			if (processing.isNotStarted()) {
				for (Step inputStep : step.getInputSteps()) {
					processing.addInputProcessing(processingsByStep.get(inputStep));
				}
			}
		}

		// save processings
		Iterable<Processing> processingsToExecute = processingRepository.saveAll(processingsByStep.values());

		Collection<UUID> processingIds = new ArrayList<>();

		// execute processors
		for (Processing processingToExecute : processingsToExecute) {
			if (processingToExecute.isNotStarted()) {
				try {
					processingRunner.asyncExecute(processingToExecute.getId());
				} catch (IllegalStateException | NoSuchElementException e) {
					log.error(String.format("Failed to execute processing %s.", processingToExecute.getId()), e);
				}
			}
			processingIds.add(processingToExecute.getId());
		}
		return processingIds;
	}
}
