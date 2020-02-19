package de.uni_jena.cs.fusion.abecto.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRestController;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;
import de.uni_jena.cs.fusion.abecto.step.StepRestController;

@RestController
public class ProjectRestControler {
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	KnowledgeBaseRestController knowledgeBaseRestController;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	StepRestController stepRestController;
	@Autowired
	ProjectRunner projectRunner;

	@PostMapping("/project")
	public Project create(@RequestParam(name = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Project project = this.get(uuid);
		for (Step step : stepRepository.findAllByProject(project)) {
			stepRestController.delete(step.getId());
		}
		for (KnowledgeBase knowledgeBase : knowledgeBaseRepository.findAllByProject(project)) {
			knowledgeBaseRestController.delete(knowledgeBase.getId());
		}
		projectRepository.delete(project);
	}

	@GetMapping("/project/{uuid}")
	public Project get(@PathVariable("uuid") UUID projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
	}

	/**
	 * Runs all subsequent {@link Step}s of the last source {@link Step}
	 * {@link Processing}s.
	 * 
	 * @param projectUuid id of the {@link Project}
	 * @return
	 */
	@GetMapping("/project/{uuid}/run")
	public Collection<Processing> run(@PathVariable("uuid") UUID projectUuid,
			@RequestParam(name = "await", defaultValue = "false") boolean await) {
		try {
			Project project = projectRepository.findById(projectUuid).orElseThrow();
			Collection<UUID> sourceKnowledgeBaseProcessingIds = new ArrayList<>();
			for (KnowledgeBase knowledgeBase : knowledgeBaseRepository.findAllByProject(project)) {
				for (Step sourceStep : knowledgeBase.getSources()) {
					try {
						sourceKnowledgeBaseProcessingIds
								.add(processingRepository.findTopByStepOrderByStartDateTimeDesc(sourceStep).getId());
					} catch (NoSuchElementException e) {
						throw new ResponseStatusException(HttpStatus.NOT_FOUND,
								String.format("No processing of %s not found.", sourceStep));
					}
				}
			}
			try {
				return projectRunner.execute(projectUuid, sourceKnowledgeBaseProcessingIds, await);
			} catch (InterruptedException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Waiting for termination of Project execution interrupted.", e);
			}
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		}
	}

	@GetMapping("/project")
	public Iterable<Project> list() {
		return projectRepository.findAll();
	}
}
