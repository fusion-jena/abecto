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

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.model.ModelRepository;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

@RestController
public class ExecutionRestController {
	@Autowired
	ExecutionRepository executionRepository;
	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
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
			@RequestParam(name = "knowledgebase", required = true) UUID knowledgeBaseId) throws NoSuchElementException,
			IllegalStateException, NullPointerException, IllegalArgumentException, ReflectiveOperationException {
		Execution execution = getExecution(executionId);
		KnowledgeBase knowledeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
				.orElseThrow(() -> new NoSuchElementException("Knowledge base not found."));
		Model metaModel = metaModel(execution);
		Category category = SparqlEntityManager.selectOne(new Category(categoryName, null, knowledgeBaseId), metaModel)
				.orElseThrow(() -> new NoSuchElementException("Category not found."));
		return category.getCategoryData(dataModel(execution, knowledeBase));
	}

	private Model dataModel(Execution execution, KnowledgeBase knowlegeBase) {
		Collection<Model> models = execution.getProcessings().stream().filter((processing) -> {
			return !processing.getStep().isMeta() && processing.getStep().getKnowledgeBase().equals(knowlegeBase);
		}).map(Processing::getModelHash).map(modelRepository::get).collect(Collectors.toList());
		return Models.union(models);
	}

	private Class<?> getModelClass(String modelClassName) throws ResponseStatusException {
		try {
			if (!modelClassName.contains(".")) {
				modelClassName = "de.uni_jena.cs.fusion.abecto.processor.model." + modelClassName;
			}
			return (Class<?>) Class.forName(modelClassName);
		} catch (ClassNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class unknown.");
		}
	}

	private Model metaModel(Execution execution) {
		Collection<Model> models = execution.getProcessings().stream().filter((processing) -> {
			return processing.getStep().isMeta();
		}).map(Processing::getModelHash).map(modelRepository::get).collect(Collectors.toList());
		return Models.union(models);
	}

}
