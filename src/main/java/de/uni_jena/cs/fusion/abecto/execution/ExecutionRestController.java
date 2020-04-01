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
package de.uni_jena.cs.fusion.abecto.execution;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.model.ModelRepository;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.ontology.OntologyRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

@RestController
public class ExecutionRestController {
	@Autowired
	ExecutionRepository executionRepository;
	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	ModelRepository modelRepository;

	@GetMapping("/execution/{uuid}")
	public Execution getExecution(@PathVariable("uuid") UUID executionId) {
		return executionRepository.findById(executionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found."));
	}

	@DeleteMapping("/execution/{uuid}")
	public void delete(@PathVariable("uuid") UUID executionId) {
		executionRepository.delete(this.getExecution(executionId));
	}

	@GetMapping("/execution/{uuid}/results")
	public Collection<Object> getResults(@PathVariable("uuid") UUID executionId,
			@RequestParam(name = "type", required = true) String modelClassName)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		Execution execution = getExecution(executionId);
		Class<?> modelClass = getModelClass(modelClassName);
		Object prototype = modelClass.getDeclaredConstructor().newInstance();
		Model metaModel = metaModel(execution);
		return SparqlEntityManager.select(prototype, metaModel);
	}

	@GetMapping("/execution/{uuid}/data")
	public Map<String, Map<String, Set<String>>> getData(@PathVariable("uuid") UUID executionId,
			@RequestParam(name = "category", required = true) String categoryName,
			@RequestParam(name = "ontology", required = true) UUID ontologyId) throws NoSuchElementException,
			IllegalStateException, NullPointerException, IllegalArgumentException, ReflectiveOperationException {
		Execution execution = getExecution(executionId);
		Ontology knowledeBase = ontologyRepository.findById(ontologyId)
				.orElseThrow(() -> new NoSuchElementException("Ontology not found."));
		Model metaModel = metaModel(execution);
		Category category = SparqlEntityManager.selectOne(new Category(categoryName, null, ontologyId), metaModel)
				.orElseThrow(() -> new NoSuchElementException("Category not found."));
		return category.getCategoryData(dataModel(execution, knowledeBase));
	}

	private Model dataModel(Execution execution, Ontology knowlegeBase) {
		Collection<Model> models = execution.getProcessings().stream().filter((processing) -> {
			return !processing.getNode().isMeta() && processing.getNode().getOntology().equals(knowlegeBase);
		}).map(Processing::getModelHash).map(modelRepository::get).collect(Collectors.toList());
		return Models.union(models);
	}

	private Class<?> getModelClass(String modelClassName) throws ResponseStatusException {
		try {
			if (!modelClassName.contains(".")) {
				modelClassName = "de.uni_jena.cs.fusion.abecto.metaentity." + modelClassName;
			}
			return (Class<?>) Class.forName(modelClassName);
		} catch (ClassNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class unknown.");
		}
	}

	private Model metaModel(Execution execution) {
		Collection<Model> models = execution.getProcessings().stream().filter((processing) -> {
			return processing.getNode().isMeta();
		}).map(Processing::getModelHash).map(modelRepository::get).collect(Collectors.toList());
		return Models.union(models);
	}

}
