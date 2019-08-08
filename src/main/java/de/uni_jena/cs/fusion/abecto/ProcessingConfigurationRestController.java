package de.uni_jena.cs.fusion.abecto;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfiguration;
import de.uni_jena.cs.fusion.abecto.processing.configuration.ProcessingConfigurationRepository;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameter;
import de.uni_jena.cs.fusion.abecto.processing.parameter.ProcessingParameterRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.project.knowledgebase.KnowledgeBaseRepository;

@RestController
@Transactional
public class ProcessingConfigurationRestController {

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	ProcessingConfigurationRepository processingConfigurationRepository;
	@Autowired
	ProcessingParameterRepository processingParameterRepository;

	@PostMapping("/source")
	public ProcessingConfiguration createForSource(@RequestParam("class") String processorClassName,
			@RequestParam("knowledgebase") UUID knowledgebaseId) {

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
	public ProcessingConfiguration createForProcessing(@RequestParam("class") String processorClassName,
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
	public ProcessingConfiguration get(@PathVariable("uuid") UUID uuid) {
		Optional<ProcessingConfiguration> configuration = processingConfigurationRepository.findById(uuid);
		if (configuration.isPresent()) {
			return configuration.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					String.format("Processing or Source %s not found.", uuid));
		}
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
