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
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;

@RestController
public class AbectoRestController {

	@Autowired
	ProjectRepository projectRepository;

	@RequestMapping("/project/create")
	public Project projectCreate(@RequestParam(value = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@RequestMapping("/project/delete/{project}")
	public ResponseEntity<?> projectDelete(@PathVariable("project") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			projectRepository.delete(project.get());
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/project/{project}")
	public ResponseEntity<Project> projectGet(@PathVariable("project") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			return ResponseEntity.ok(project.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/project/list")
	public Iterable<Project> projectList() {
		return projectRepository.findAll();
	}

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;

	@RequestMapping("/knowledgebase/create")
	public ResponseEntity<KnowledgeBase> knowledgeBaseCreate(@RequestParam(value = "project") UUID projectId,
			@RequestParam(value = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return ResponseEntity.ok(knowledgeBaseRepository.save(new KnowledgeBase(project.get(), label)));
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/knowledgebase/delete/{knowledgebase}")
	public ResponseEntity<?> knowledgeBaseDelete(@PathVariable("knowledgebase") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			knowledgeBaseRepository.delete(knowledgeBase.get());
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/knowledgebase/{knowledgebase}")
	public ResponseEntity<KnowledgeBase> knowledgeBaseGet(@PathVariable("knowledgebase") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			return ResponseEntity.ok(knowledgeBase.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping("/knowledgebase/list")
	public ResponseEntity<Iterable<KnowledgeBase>> knowledgeBaseList(
			@RequestParam(value = "project", required = false) UUID projectId) {
		if (projectId != null) {
			Optional<Project> project = projectRepository.findById(projectId);
			if (project.isPresent()) {
				return ResponseEntity.ok(knowledgeBaseRepository.findAllByProject(project.get()));
			} else {
				return ResponseEntity.notFound().build();
			}
		} else {
			return ResponseEntity.ok(knowledgeBaseRepository.findAll());
		}
	}

}
