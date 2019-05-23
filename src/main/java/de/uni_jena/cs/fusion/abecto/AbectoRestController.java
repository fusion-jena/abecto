package de.uni_jena.cs.fusion.abecto;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.source.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;

@RestController
public class AbectoRestController {

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	ProcessingConfigurationRepository processingConfigurationRepository;
	@Autowired
	ProcessingParameterRepository processingParameterRepository;
	@Autowired
	ProjectRepository projectRepository;

	@RequestMapping("/knowledgebase/create")
	public ResponseEntity<KnowledgeBase> knowledgeBaseCreate(@RequestParam(value = "project") UUID projectId,
			@RequestParam(value = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return ResponseEntity.ok(knowledgeBaseRepository.save(new KnowledgeBase(project.get(), label)));
		} else {
			return ResponseEntity.badRequest().build();
		}
	}

	@RequestMapping("/knowledgebase/delete")
	public ResponseEntity<?> knowledgeBaseDelete(@RequestParam(value = "id") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			knowledgeBaseRepository.delete(knowledgeBase.get());
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.badRequest().build();
		}
	}

	@RequestMapping("/knowledgebase/get")
	public ResponseEntity<KnowledgeBase> knowledgeBaseGet(@RequestParam(value = "id") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			return ResponseEntity.ok(knowledgeBase.get());
		} else {
			return ResponseEntity.badRequest().build();
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
				return ResponseEntity.badRequest().build();
			}
		} else {
			return ResponseEntity.ok(knowledgeBaseRepository.findAll());
		}
	}

	@RequestMapping("/project/create")
	public Project projectCreate(@RequestParam(value = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@RequestMapping("/project/delete")
	public ResponseEntity<?> projectDelete(@RequestParam(value = "id") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			projectRepository.delete(project.get());
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.badRequest().build();
		}
	}

	@RequestMapping("/project/get")
	public ResponseEntity<Project> projectGet(@RequestParam(value = "id") UUID uuid) {
		Optional<Project> project = projectRepository.findById(uuid);
		if (project.isPresent()) {
			return ResponseEntity.ok(project.get());
		} else {
			return ResponseEntity.badRequest().build();
		}
	}

	@RequestMapping("/project/list")
	public Iterable<Project> projectList() {
		return projectRepository.findAll();
	}

	@PostMapping("/source")
	public ProcessingConfiguration processingConfigurationCreate(
			@RequestParam(value = "class") String processorClassName,
			@RequestParam(value = "parameters") String parameters,
			@RequestParam(value = "knowledgebase") UUID knowledgebaseId) {

		Class<SourceProcessor> processorClass = getProcessorClass(processorClassName, SourceProcessor.class);

		KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgebaseId).orElseThrow();

		return processingConfigurationRepository
				.save(new ProcessingConfiguration(processorClass,
						processingParameterRepository.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu.owl")),
						knowledgeBase));
		// TODO specify exception thrown

	}

	@PostMapping("/processing")
	public ProcessingConfiguration processingConfigurationCreate(
			@RequestParam(value = "class") String processorClassName,
			@RequestParam(value = "parameters") String parameters,
			@RequestParam(value = "input") Collection<UUID> configurationIds) {

		Class<RefinementProcessor> processorClass = getProcessorClass(processorClassName, RefinementProcessor.class);

		for (UUID configurationId : configurationIds) {
			if (!processingConfigurationRepository.existsById(configurationId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						String.format("Processing configuration %s not found.", configurationId));
			}
		}
		Iterable<ProcessingConfiguration> inputConfigurations = processingConfigurationRepository
				.findAllById(configurationIds);

		return processingConfigurationRepository
				.save(new ProcessingConfiguration(processorClass,
						processingParameterRepository.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu.owl")),
						inputConfigurations));
	}

	@SuppressWarnings("unchecked")
	private <T extends Processor> Class<T> getProcessorClass(String processorClassName, Class<T> processorInterface)
			throws ResponseStatusException {
		try {
			if (!processorClassName.contains(".")) {
				processorClassName = "de.uni_jena.cs.fusion.abecto.processor." + processorClassName;
			}

			return (Class<T>) Class.forName(processorClassName);
		} catch (ClassNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processor class not found.");
		} catch (ClassCastException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String
					.format("Processor class not appropriate: Expected implementation of \"%s\".", processorInterface));
		}
	}
}
