package de.uni_jena.cs.fusion.abecto;

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

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
@Transactional
public class ProjectRestControler {
	@Autowired
	ProjectRepository projectRepository;

	@PostMapping("/project")
	public Project projectCreate(@RequestParam(name = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void projectDelete(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			projectRepository.delete(project.get());
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		}
	}

	@GetMapping("/project/{uuid}")
	public Project projectGet(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			return project.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
		}
	}

	@GetMapping("/project")
	public Iterable<Project> projectList() {
		return projectRepository.findAll();
	}
}
