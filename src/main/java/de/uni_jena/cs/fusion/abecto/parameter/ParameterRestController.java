package de.uni_jena.cs.fusion.abecto.parameter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.Abecto;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;
import de.uni_jena.cs.fusion.abecto.step.Step;
import de.uni_jena.cs.fusion.abecto.step.StepRepository;

@RestController
@Transactional
public class ParameterRestController {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);
	
	@Autowired
	ObjectMapper JSON;
	@Autowired
	StepRepository stepRepository;
	@Autowired
	ParameterRepository parameterRepository;
	@Autowired
	ProjectRepository projectRepository;

	@PostMapping({ "/step/{step}/parameter" })
	public void add(@PathVariable("step") UUID stepId,
			@RequestParam(name = "key", required = false) String parameterPath,
			@RequestParam(name = "value", required = false) String parameterValue) {

		Step step = stepRepository.findById(stepId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processing or Source not found.");
					}
				});

		try {
			if (step.getParameter().containsKey(parameterPath)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter already set.");
			} else {
				try {
					// copy parameters
					Parameter newParameter = step.getParameter().copy();
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
					// update step and persist
					step.setParameter(parameterRepository.save(newParameter));
					stepRepository.save(step);
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

	@DeleteMapping({ "/step/{step}/parameter" })
	public void delete(@PathVariable("step") UUID stepId) {
		// TODO
	}

	@GetMapping({ "/step/{step}/parameter" })
	public Object get(@PathVariable("step") UUID stepId,
			@RequestParam(name = "key", required = false) String parameterPath) {
		Parameter parameter = stepRepository.findById(stepId)
				.orElseThrow(new Supplier<ResponseStatusException>() {
					@Override
					public ResponseStatusException get() {
						return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processing or Source not found.");
					}
				}).getParameter();

		if (parameterPath == null) {
			return parameter;
		} else {
			try {
				return parameter.get(parameterPath);
			} catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
				return new ResponseStatusException(HttpStatus.BAD_REQUEST,
						String.format("Parameter \"%s\" not found.", parameterPath));
			}
		}
	}

	@PutMapping({ "/step/{step}/parameter" })
	public void update(@PathVariable("step") UUID stepId) {
		// TODO
	}

}
