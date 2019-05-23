package de.uni_jena.cs.fusion.abecto;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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

	@PostMapping("/knowledgebase")
	public KnowledgeBase knowledgeBaseCreate(@RequestParam(value = "project") UUID projectId,
			@RequestParam(value = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return knowledgeBaseRepository.save(new KnowledgeBase(project.get(), label));
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
		}
	}

	@DeleteMapping("/knowledgebase/{uuid}")
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	public void knowledgeBaseDelete(@PathVariable("uuid") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			knowledgeBaseRepository.delete(knowledgeBase.get());
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KnowledgeBase not found.");
		}
	}

	@GetMapping("/knowledgebase/{uuid}")
	public KnowledgeBase knowledgeBaseGet(@PathVariable("uuid") UUID uuid) {
		Optional<KnowledgeBase> knowledgeBase = knowledgeBaseRepository.findById(uuid);
		if (knowledgeBase.isPresent()) {
			return knowledgeBase.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KnowledgeBase not found.");
		}
	}

	@GetMapping("/knowledgebase")
	public Iterable<KnowledgeBase> knowledgeBaseList(
			@RequestParam(value = "project", required = false) UUID projectId) {
		if (projectId != null) {
			Optional<Project> project = projectRepository.findById(projectId);
			if (project.isPresent()) {
				return knowledgeBaseRepository.findAllByProject(project.get());
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
			}
		} else {
			return knowledgeBaseRepository.findAll();
		}
	}

	@PostMapping("/project")
	public Project projectCreate(@RequestParam(value = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
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

	@PostMapping("/source")
	public ProcessingConfiguration processingConfigurationCreate(
			@RequestParam(value = "class") String processorClassName,
			@RequestParam(value = "parameters") String parameters,
			@RequestParam(value = "knowledgebase") UUID knowledgebaseId) {

		Class<SourceProcessor> processorClass = getProcessorClass(processorClassName, SourceProcessor.class);

		KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgebaseId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "KnowledgeBase not found.");
					}
				});

		return processingConfigurationRepository
				.save(new ProcessingConfiguration(processorClass,
						processingParameterRepository.save(new ProcessingParameter().set("path",
								"C:\\Users\\admin\\Documents\\Workspace\\unit-ontologies\\qu\\qu.owl")),
						knowledgeBase));

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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processor class unknown.");
		} catch (ClassCastException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String
					.format("Processor class not appropriate: Expected implementation of \"%s\".", processorInterface));
		}
	}
}
