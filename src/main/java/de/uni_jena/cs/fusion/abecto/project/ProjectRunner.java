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
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	ProcessingRunner processorRunner;

	/**
	 * Schedules a given {@link Project} starting at the given {@link Processing}s.
	 * The given {@link Processing}s and their dependent {@link Processing}s will
	 * not be executed.
	 * 
	 * @param project     {@link Project} to execute the belonging pipeline
	 * @param processings {@link Processing}s to start at
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void execute(UUID projectId, Collection<UUID> startProcessingIds, boolean await) throws InterruptedException, ExecutionException {
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

		// create new processing for missing steps
		for (Step step : steps) {
			processingsByStep.computeIfAbsent(step, (c) -> new Processing(c));
		}

		// save processings
		Iterable<Processing> processingsToExecute = processingRepository.saveAll(processingsByStep.values());

		Collection<Future<Processing>> futures = new ArrayList<>();
		// execute processors
		for (Processing processingToExecute : processingsToExecute) {
			if (processingToExecute.isNotStarted()) {
				try {
					futures.add(processorRunner.asyncExecute(processingToExecute.getId()));
				} catch (IllegalStateException | NoSuchElementException e) {
					log.error(String.format("Failed to execute processing %s.", processingToExecute.getId()), e);
				}
			}
		}

		if (await) {
			for (Future<Processing> future : futures) {
				future.get();
			}
		}

	}
}
