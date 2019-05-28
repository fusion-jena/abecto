package de.uni_jena.cs.fusion.abecto;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;

@RestController
@Transactional
public class AbectoRestController {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

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
	public ProcessingConfiguration processingConfigurationCreateSource(
			@RequestParam(value = "class") String processorClassName,
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
						processingParameterRepository.save(new ProcessingParameter()),
						knowledgeBase));
	}

	@PostMapping("/processing")
	public ProcessingConfiguration processingConfigurationCreateProcessing(
			@RequestParam(value = "class") String processorClassName,
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
						processingParameterRepository.save(new ProcessingParameter()),
						inputConfigurations));
	}

	@PostMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationAddParameter(@PathVariable("configuration") UUID configurationId,
			@RequestParam(value = "key", required = false) String parameterPath,
			@RequestParam(value = "value", required = false) String parameterValue) {

		ProcessingConfiguration configuration = processingConfigurationRepository.findById(configurationId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Processing or Source not found.");
					}
				});

		if (configuration.getProcessingParameter().containsKey(parameterPath)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter already set.");
		} else {
			try {
				// copy parameter
				ProcessingParameter newParameter = configuration.getProcessingParameter().copy();
				// update parameter
				newParameter.put(parameterPath, parameterValue);
				// update configuration
				configuration.setParameter(newParameter);
				// persist changes
				processingConfigurationRepository.save(configuration);
				processingParameterRepository.save(newParameter);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.error("Failed to copy ProcessingParameter.", e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Failed to copy parameter.");
			}
		}
	}

	@PutMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationUpdateParameter(@PathVariable("configuration") UUID configurationId) {

		// TODO
	}

	@DeleteMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationDeleteParameter(@PathVariable("configuration") UUID configurationId) {
		// TODO
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
