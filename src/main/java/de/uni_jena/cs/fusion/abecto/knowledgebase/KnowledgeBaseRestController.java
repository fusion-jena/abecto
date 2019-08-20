package de.uni_jena.cs.fusion.abecto.knowledgebase;

import java.util.Optional;
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

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
public class KnowledgeBaseRestController {

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	ProjectRepository projectRepository;

	@PostMapping("/knowledgebase")
	public KnowledgeBase create(@RequestParam("project") UUID projectId,
			@RequestParam(name = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return knowledgeBaseRepository.save(new KnowledgeBase(project.get(), label));
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
		}
	}

	@DeleteMapping("/knowledgebase/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			knowledgeBaseRepository.delete(knowledgeBase.get());
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KnowledgeBase not found.");
		}
	}

	@GetMapping("/knowledgebase/{uuid}")
	public KnowledgeBase get(@PathVariable("uuid") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			return knowledgeBase.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KnowledgeBase not found.");
		}
	}

	@GetMapping("/knowledgebase")
	public Iterable<KnowledgeBase> list(@RequestParam(name = "project", required = false) UUID projectId) {
		if (projectId != null) {
			Optional<Project> project = projectRepository.findById(projectId);
			if (project.isPresent()) {
				return knowledgeBaseRepository.findAllByProject(project.get());
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
			}
		} else {
			return knowledgeBaseRepository.findAll();
		}
	}

}
