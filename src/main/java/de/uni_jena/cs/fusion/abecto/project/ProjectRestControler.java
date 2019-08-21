package de.uni_jena.cs.fusion.abecto.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.step.Step;

@RestController
public class ProjectRestControler {
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProjectRunner projectRunner;

	@PostMapping("/project")
	public Project create(@RequestParam(name = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			projectRepository.delete(project.get());
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		}
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
	 */
	@GetMapping("/project/{uuid}/run")
	public void run(@PathVariable("uuid") UUID projectUuid,
			@RequestParam(name = "await", defaultValue = "false") boolean await) {
		try {
			Project project = projectRepository.findById(projectUuid).orElseThrow();
			Collection<UUID> sourceKnowledgeBaseProcessingIds = new ArrayList<>();
			for (KnowledgeBase knowledgeBase : project.knowledgeBases) {
				for (Step sourceStep : knowledgeBase.getSources()) {
					sourceKnowledgeBaseProcessingIds.add(sourceStep.getLastProcessing().getId());
				}
			}
			projectRunner.execute(projectUuid, sourceKnowledgeBaseProcessingIds, await);
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		} catch (ExecutionException | InterruptedException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project execution failed.", e);
		}
	}

	@GetMapping("/project")
	public Iterable<Project> list() {
		return projectRepository.findAll();
	}
}
