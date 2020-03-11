package de.uni_jena.cs.fusion.abecto.step;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.ontology.Ontology;
import de.uni_jena.cs.fusion.abecto.ontology.OntologyRepository;
import de.uni_jena.cs.fusion.abecto.parameter.Parameter;
import de.uni_jena.cs.fusion.abecto.parameter.ParameterRepository;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRestController;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRunner;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.project.Project;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
public class StepRestController {

	@Autowired
	ProcessingRunner processorRunner;
	@Autowired
	ObjectMapper JSON;

	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ProcessingRestController processingRestController;
	@Autowired
	ParameterRepository parameterRepository;

	/**
	 * Creates a new Refinement Processor Node in the processing pipeline.
	 * 
	 * @param processorClassName
	 * @param inputStepIds
	 * @return
	 */
	@PostMapping("/step")
	public Step create(@RequestParam("class") String processorClassName,
			@RequestParam(name = "ontology", required = false) UUID ontologyId,
			@RequestParam(name = "input", required = false) Collection<UUID> inputStepIds,
			@RequestParam(name = "parameters", required = false) String parameterJson) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		if (SourceProcessor.class.isAssignableFrom(processorClass)) {
			// check input parameter
			if (inputStepIds != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Parameter \"input\" not permited for SourceProcessors.");
			}

			// check ontology parameter
			Ontology ontology = ontologyRepository.findById(ontologyId)
					.orElseThrow(new Supplier<ResponseStatusException>() {
						@Override
						public ResponseStatusException get() {
							return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ontology not found.");
						}
					});
			// create step
			Parameter parameter = parameterRepository.save(new Parameter(getParameter(processorClass, parameterJson)));
			return stepRepository.save(new Step(processorClass, parameter, ontology));

		} else {
			// check ontology parameter
			if (ontologyId != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Parameter \"ontology\" only permited for SourceProcessors.");
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
	public Processing load(@PathVariable("uuid") UUID stepId,
			@RequestParam(name = "file", required = false) MultipartFile file) {
		Step step = get(stepId);
		Processing processing = processingRepository.save(new Processing(step));
		try {
			if (file == null) {
				processorRunner.syncExecute(processing);
			} else {
				processorRunner.syncExecute(processing, file.getInputStream());
			}
		} catch (IllegalArgumentException | IllegalStateException | IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Failed to execute Processor for Processing %s.", stepId), e);
		}
		return processing;
	}

	@GetMapping("/step/{uuid}/processing")
	public Iterable<Processing> processings(@PathVariable("uuid") UUID stepId) {
		return processingRepository.findByStepOrderByStartDateTime(get(stepId));
	}

	@DeleteMapping("/step/{uuid}")
	public void delete(@PathVariable("uuid") UUID uuid) {
		Step step = this.get(uuid);
		for (Processing processing : processingRepository.findAllByStep(step)) {
			processingRestController.delete(processing.getId());
		}
		stepRepository.delete(step);
	}

	@GetMapping("/step/{uuid}/processing/last")
	public Processing lastProcessing(@PathVariable("uuid") UUID stepId) {
		return processingRepository.findTopByStepOrderByStartDateTimeDesc(get(stepId));
	}

	@GetMapping("/step")
	public Iterable<Step> list(@RequestParam(name = "project") UUID projectId) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return stepRepository.findAllByProject(project.get());
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found.");
		}
	}

	@SuppressWarnings("unchecked")
	private Class<Processor<?>> getProcessorClass(String processorClassName) throws ResponseStatusException {
		try {
			if (!processorClassName.contains(".")) {
				processorClassName = "de.uni_jena.cs.fusion.abecto.processor.implementation." + processorClassName;
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
