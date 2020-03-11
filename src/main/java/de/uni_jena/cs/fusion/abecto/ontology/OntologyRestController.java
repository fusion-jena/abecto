package de.uni_jena.cs.fusion.abecto.ontology;

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
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;
import de.uni_jena.cs.fusion.abecto.step.StepRestController;

@RestController
public class OntologyRestController {

	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	StepRestController stepRestController;

	@PostMapping("/ontology")
	public Ontology create(@RequestParam("project") UUID projectId,
			@RequestParam(name = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return ontologyRepository.save(new Ontology(project.get(), label));
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
		}
	}

	@DeleteMapping("/ontology/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Ontology ontology = this.get(uuid);
		for (Step step : stepRepository.findAllByOntology(ontology)) {
			stepRestController.delete(step.getId());
		}
		ontologyRepository.delete(ontology);
	}

	@GetMapping("/ontology/{uuid}")
	public Ontology get(@PathVariable("uuid") UUID uuid) {
		Optional<Ontology> ontology = ontologyRepository.findById(uuid);
		if (ontology.isPresent()) {
			return ontology.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ontology not found.");
		}
	}

	@GetMapping("/ontology")
	public Iterable<Ontology> list(@RequestParam(name = "project", required = false) UUID projectId) {
		if (projectId != null) {
			Optional<Project> project = projectRepository.findById(projectId);
			if (project.isPresent()) {
				return ontologyRepository.findAllByProject(project.get());
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
			}
		} else {
			return ontologyRepository.findAll();
		}
	}

}
