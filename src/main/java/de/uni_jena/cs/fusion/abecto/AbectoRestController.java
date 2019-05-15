package de.uni_jena.cs.fusion.abecto;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
public class AbectoRestController {

	@Autowired
	ProjectRepository projectRepository;

	@RequestMapping("/project/create")
	public Project projectCreate(@RequestParam(value = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@RequestMapping("/project/delete/{uuid}")
	public ResponseEntity<?> projectDelete(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			projectRepository.delete(project.get());
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/project/get/{uuid}")
	public ResponseEntity<Project> projectGet(@PathVariable("uuid") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			System.out.print(project.get());
			return ResponseEntity.ok(project.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/project")
	public Iterable<Project> projectList() {
		return projectRepository.findAll();
	}

}
