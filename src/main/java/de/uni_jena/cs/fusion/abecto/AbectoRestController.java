package de.uni_jena.cs.fusion.abecto;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;

@RestController
@Transactional
public class AbectoRestController {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);
	private static final ObjectMapper JSON = new ObjectMapper();

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	ProcessingConfigurationRepository processingConfigurationRepository;
	@Autowired
	ProcessingParameterRepository processingParameterRepository;
	@Autowired
	ProjectRepository projectRepository;

	@PostMapping("/knowledgebase")
	public KnowledgeBase knowledgeBaseCreate(@RequestParam("project") UUID projectId,
			@RequestParam(name = "label", defaultValue = "") String label) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return knowledgeBaseRepository.save(new KnowledgeBase(project.get(), label));
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
		}
	}

	@DeleteMapping("/knowledgebase/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
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
	public Iterable<KnowledgeBase> knowledgeBaseList(@RequestParam(name = "project", required = false) UUID projectId) {
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
	public Project projectCreate(@RequestParam(name = "label", defaultValue = "") String label) {
		return projectRepository.save(new Project(label));
	}

	@DeleteMapping("/project/{uuid}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
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
	public ProcessingConfiguration processingConfigurationCreateForSource(
			@RequestParam("class") String processorClassName, @RequestParam("knowledgebase") UUID knowledgebaseId) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgebaseId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "KnowledgeBase not found.");
					}
				});

		try {
			ProcessingParameter defaultParameter = processingParameterRepository
					.save(new ProcessingParameter(processorClass));

			return processingConfigurationRepository
					.save(new ProcessingConfiguration(processorClass, defaultParameter, knowledgeBase));
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to load requested processor.", e);
		}
	}

	/**
	 * Creates a new Refinement Processor Node in the processing pipeline.
	 * 
	 * @param processorClassName
	 * @param configurationIds
	 * @return
	 */
	@PostMapping("/processing")
	public ProcessingConfiguration processingConfigurationCreateForProcessing(
			@RequestParam("class") String processorClassName,
			@RequestParam("input") Collection<UUID> configurationIds) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		for (UUID configurationId : configurationIds) {
			if (!processingConfigurationRepository.existsById(configurationId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						String.format("Processing configuration %s not found.", configurationId));
			}
		}
		Iterable<ProcessingConfiguration> inputConfigurations = processingConfigurationRepository
				.findAllById(configurationIds);
		try {
			ProcessingParameter defaultParameter = processingParameterRepository
					.save(new ProcessingParameter(processorClass));

			return processingConfigurationRepository
					.save(new ProcessingConfiguration(processorClass, defaultParameter, inputConfigurations));
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to load requested processor.", e);
		}
	}

	@GetMapping({ "/source/{uuid}", "/processing/{uuid}" })
	public ProcessingConfiguration processingConfigurationGet(@PathVariable("uuid") UUID uuid) {
		Optional<ProcessingConfiguration> configuration = processingConfigurationRepository.findById(uuid);
		if (configuration.isPresent()) {
			return configuration.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					String.format("Processing or Source %s not found.", uuid));
		}
	}

	@PostMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationAddParameter(@PathVariable("configuration") UUID configurationId,
			@RequestParam(name = "key", required = false) String parameterPath,
			@RequestParam(name = "value", required = false) String parameterValue) {

		ProcessingConfiguration configuration = processingConfigurationRepository.findById(configurationId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processing or Source not found.");
					}
				});

		try {
			if (configuration.getProcessingParameter().containsKey(parameterPath)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter already set.");
			} else {
				try {
					// copy parameters
					ProcessingParameter newParameter = configuration.getProcessingParameter().copy();
					// get type of changed parameter
					Class<?> type = newParameter.getType(parameterPath);
					try {
						// pares new value
						Object value = JSON.readValue(parameterValue, type);
						// update parameters
						newParameter.put(parameterPath, value);
					} catch (IllegalArgumentException | IOException e) {
						log.error("Failed to parse input value.", e);
						throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
								String.format("Failed to pares value of type \"%s\".", type));
					}
					// update configuration
					configuration.setParameter(newParameter);
					// persist changes
					processingConfigurationRepository.save(configuration);
					processingParameterRepository.save(newParameter);
				} catch (SecurityException | InstantiationException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException e) {
					log.error("Failed to copy ProcessingParameter.", e);
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to copy parameters.");
				}
			}
		} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
			log.error("Failed to check parameter status.", e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check parameter status.");
		}
	}

	@DeleteMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationDeleteParameter(@PathVariable("configuration") UUID configurationId) {
		// TODO
	}

	@PostMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationGetParameter(@PathVariable("configuration") UUID configurationId,
			@RequestParam(name = "key", required = false) String parameterPath) {

	}

	@PutMapping({ "/source/{configuration}/parameter", "/processing/{configuration}/parameter" })
	public void processingConfigurationUpdateParameter(@PathVariable("configuration") UUID configurationId) {

		// TODO
	}

	@SuppressWarnings("unchecked")
	private Class<Processor<?>> getProcessorClass(String processorClassName) throws ResponseStatusException {
		try {
			if (!processorClassName.contains(".")) {
				processorClassName = "de.uni_jena.cs.fusion.abecto.processor." + processorClassName;
			}

			return (Class<Processor<?>>) Class.forName(processorClassName);
		} catch (ClassNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processor class unknown.");
		}
	}
}
