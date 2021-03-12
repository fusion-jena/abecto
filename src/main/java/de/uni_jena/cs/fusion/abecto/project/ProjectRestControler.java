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
package de.uni_jena.cs.fusion.abecto.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.execution.Execution;
import de.uni_jena.cs.fusion.abecto.node.Node;
import de.uni_jena.cs.fusion.abecto.node.NodeRepository;
import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.ontology.OntologyRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;

@RestController
public class ProjectRestControler {
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	NodeRepository nodeRepository;
	@Autowired
	ProjectRunner projectRunner;

	@PostMapping("/project")
	public Project create(@RequestParam(name = "name") String projectName) {
		try {
			return projectRepository.save(new Project(projectName));
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already used.");
		}
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("uuid") UUID uuid) {
		Project project = this.get(uuid);
		nodeRepository.deleteAllByProject(project);
		ontologyRepository.deleteAllByProject(project);
		projectRepository.delete(project);
	}

	@GetMapping("/project/{uuid}")
	public Project get(@PathVariable("uuid") UUID projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
	}

	@GetMapping(value = "/project", params = "name")
	public Project getByName(@RequestParam(name = "name") String projectName) {
		return projectRepository.findOneByName(projectName)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
	}

	/**
	 * Runs all subsequent {@link Node}s of the last source {@link Node}
	 * {@link Processing}s.
	 * 
	 * @param projectUuid id of the {@link Project}
	 * @return
	 */
	@GetMapping("/project/{uuid}/run")
	public Execution run(@PathVariable("uuid") UUID projectUuid,
			@RequestParam(name = "await", defaultValue = "false") boolean await) {
		try {
			Project project = projectRepository.findById(projectUuid).orElseThrow();
			Collection<UUID> sourceOntologyProcessingIds = new ArrayList<>();
			for (Ontology ontology : ontologyRepository.findAllByProject(project)) {
				for (Node node : ontology.getNodes()) {
					if (node.isSource()) {
						try {
							sourceOntologyProcessingIds
									.add(processingRepository.findTopByNodeOrderByStartDateTimeDesc(node).getId());
						} catch (NoSuchElementException e) {
							throw new ResponseStatusException(HttpStatus.NOT_FOUND,
									String.format("Processing of %s not found.", node));
						}
					}
				}
			}
			try {
				return projectRunner.execute(projectUuid, sourceOntologyProcessingIds, await);
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
