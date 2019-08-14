package de.uni_jena.cs.fusion.abecto.step;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBase;
import de.uni_jena.cs.fusion.abecto.knowledgebase.KnowledgeBaseRepository;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.UploadSourceProcessor;
import de.uni_jena.cs.fusion.abecto.runner.ProcessorRunner;

@RestController
@Transactional
public class StepRestController {

	@Autowired
	KnowledgeBaseRepository knowledgeBaseRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ParameterRepository parameterRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ProcessorRunner processorRunner;

	private final static ObjectMapper JSON = new ObjectMapper();

	/**
	 * Creates a new Refinement Processor Node in the processing pipeline.
	 * 
	 * @param processorClassName
	 * @param inputStepIds
	 * @return
	 */
	@PostMapping("/step")
	public Step createNotSource(@RequestParam("class") String processorClassName,
			@RequestParam(name = "knowledgebase", required = false) UUID knowledgebaseId,
			@RequestParam(name = "input", required = false) Collection<UUID> inputStepIds,
			@RequestParam(name = "parameters", required = false) String parameterJson) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		if (SourceProcessor.class.isAssignableFrom(processorClass)) {
			// check input parameter
			if (inputStepIds != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Parameter \"input\" not permited for SourceProcessors.");
			}

			// check knowledgebase parameter
			KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgebaseId)
					.orElseThrow(new Supplier<ResponseStatusException>() {
						@Override
						public ResponseStatusException get() {
							return new ResponseStatusException(HttpStatus.BAD_REQUEST, "KnowledgeBase not found.");
						}
					});
			// create step
			Parameter parameter = parameterRepository.save(new Parameter(getParameter(processorClass, parameterJson)));
			return stepRepository.save(new Step(processorClass, parameter, knowledgeBase));

		} else {
			// check knowledgebase parameter
			if (knowledgebaseId != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Parameter \"knowledgebase\" only permited for SourceProcessors.");
			}
			// check input parameter
			for (UUID inputStepId : inputStepIds) {
				if (!stepRepository.existsById(inputStepId)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							String.format("Input step %s not found.", inputStepId));
				}
			}
			Iterable<Step> inputSteps = stepRepository.findAllById(inputStepIds);
			// create step
			Parameter parameter = parameterRepository.save(new Parameter(getParameter(processorClass, parameterJson)));
			return stepRepository.save(new Step(processorClass, parameter, inputSteps));
		}
	}

	@GetMapping({ "/step/{uuid}" })
	public Step get(@PathVariable("uuid") UUID uuid) {
		Optional<Step> step = stepRepository.findById(uuid);
		if (step.isPresent()) {
			return step.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Step %s not found.", uuid));
		}
	}

	@PostMapping("/step/{uuid}/load")
	public void load(@PathVariable("uuid") UUID uuid,
			@RequestParam(name = "file", required = false) MultipartFile file) {
		Step step = get(uuid);
		Processing processing = processingRepository.save(new Processing(step));
		try {
			Processor<?> processor = processing.getProcessorInsance();
			if (file == null) {
				if (processor instanceof UploadSourceProcessor<?>) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"SourceProcessor requires to upload an input file.");
				}
			} else {
				if (processor instanceof UploadSourceProcessor<?>) {
					try {
						((UploadSourceProcessor<?>) processor).setUploadStream(file.getInputStream());
					} catch (IOException e) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload failed.", e);
					}
				} else {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"SourceProcessor does not accepts input file uploads.");
				}
			}
			processor.setParameters(step.getParameter().getParameters());
			processorRunner.execute(processing, processor);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processor execution failed.", e);
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
