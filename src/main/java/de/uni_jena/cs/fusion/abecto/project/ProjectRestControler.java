package de.uni_jena.cs.fusion.abecto.project;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Transactional
public class ProjectRestControler {
	@Autowired
	ProjectRepository projectRepository;

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
	public Project get(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			return project.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		}
	}

	@GetMapping("/project")
	public Iterable<Project> list() {
		return projectRepository.findAll();
	}
}
