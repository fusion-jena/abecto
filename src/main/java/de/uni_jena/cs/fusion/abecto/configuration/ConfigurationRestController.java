package de.uni_jena.cs.fusion.abecto.configuration;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;

@RestController
@Transactional
public class ConfigurationRestController {

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	ConfigurationRepository configurationRepository;
	@Autowired
	ParameterRepository parameterRepository;

	private final static ObjectMapper JSON = new ObjectMapper();

	@PostMapping("/source")
	public Configuration createForSource(@RequestParam("class") String processorClassName,
			@RequestParam("knowledgebase") UUID knowledgebaseId,
			@RequestParam(name = "parameters", required = false) String parameterJson) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgebaseId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "KnowledgeBase not found.");
					}
				});

		Parameter parameter = parameterRepository
				.save(new Parameter(getParameter(processorClass, parameterJson)));
		return configurationRepository
				.save(new Configuration(processorClass, parameter, knowledgeBase));

	}

	/**
	 * Creates a new Refinement Processor Node in the processing pipeline.
	 * 
	 * @param processorClassName
	 * @param configurationIds
	 * @return
	 */
	@PostMapping("/processing")
	public Configuration createForProcessing(@RequestParam("class") String processorClassName,
			@RequestParam("input") Collection<UUID> configurationIds,
			@RequestParam(name = "parameters", required = false) String parameterJson) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		for (UUID configurationId : configurationIds) {
			if (!configurationRepository.existsById(configurationId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						String.format("Processing configuration %s not found.", configurationId));
			}
		}
		Iterable<Configuration> inputConfigurations = configurationRepository
				.findAllById(configurationIds);

		Parameter parameter = parameterRepository
				.save(new Parameter(getParameter(processorClass, parameterJson)));
		return configurationRepository
				.save(new Configuration(processorClass, parameter, inputConfigurations));
	}

	@GetMapping({ "/source/{uuid}", "/processing/{uuid}" })
	public Configuration get(@PathVariable("uuid") UUID uuid) {
		Optional<Configuration> configuration = configurationRepository.findById(uuid);
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

	private ParameterModel getParameter(Class<Processor<?>> processorClass, String parameterJson) {
		if (parameterJson == null) {
			try {
				return Processor.getDefaultParameters(processorClass);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to set default parameters.", e);
			}
		} else {
			try {
				return JSON.readValue(parameterJson, Processor.getParameterClass(processorClass));
			} catch (IOException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read parameters.", e);
			}
		}
	}
}
