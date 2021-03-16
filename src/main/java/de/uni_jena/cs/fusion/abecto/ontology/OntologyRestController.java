/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import de.uni_jena.cs.fusion.abecto.node.NodeRepository;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
public class OntologyRestController {

	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	NodeRepository nodeRepository;

	@PostMapping("/ontology")
	public Ontology create(@RequestParam("project") UUID projectId, @RequestParam(name = "name") String ontologyName,
			@RequestParam(name = "useIfExists", defaultValue = "false") boolean useIfExists) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found."));
		Optional<Ontology> existingOntology = ontologyRepository.findOneByProjectAndName(project, ontologyName);
		if (existingOntology.isPresent()) {
			if (useIfExists) {
				return existingOntology.get();
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
						"Ontology name \"%s\" already used in project \"%s\".", ontologyName, project.getName()));
			}
		} else {
			return ontologyRepository.save(new Ontology(project, ontologyName));
		}
	}

	@DeleteMapping("/ontology/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Ontology ontology = this.get(uuid);
		nodeRepository.deleteAllByOntology(ontology);
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
