package de.uni_jena.cs.fusion.abecto.parameter;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import de.uni_jena.cs.fusion.abecto.Abecto;
import de.uni_jena.cs.fusion.abecto.node.Node;
import de.uni_jena.cs.fusion.abecto.node.NodeRepository;
import de.uni_jena.cs.fusion.abecto.project.ProjectRepository;

@RestController
@Transactional
public class ParameterRestController {
	private static final Logger log = LoggerFactory.getLogger(Abecto.class);

	@Autowired
	ObjectMapper JSON;
	@Autowired
	NodeRepository nodeRepository;
	@Autowired
	ParameterRepository parameterRepository;
	@Autowired
	ProjectRepository projectRepository;

	@PostMapping("/node/{node}/parameters")
	public void set(@PathVariable("node") UUID nodeId, @RequestParam(name = "key") String parameterPath,
			@RequestParam(name = "value", required = false) String parameterValue) {

		Node node = getNode(nodeId);

		try {
			// copy parameters
			Parameter newParameter = node.getParameter().copy();
			// get type of changed parameter
			Class<?> type = newParameter.getType(parameterPath);
			try {
				// parse new value
				Object value = JSON.readValue(parameterValue, type);
				// update parameters
				newParameter.put(parameterPath, value);
			} catch (IllegalArgumentException | IOException e) {
				log.error("Failed to parse input value.", e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						String.format("Failed to pares value of type \"%s\".", type));
			}
			// update node and persist
			node.setParameter(parameterRepository.save(newParameter));
			nodeRepository.save(node);
		} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
			log.error("Failed to set parameter value.", e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set parameter value.");
		}
	}

	@GetMapping("/node/{node}/parameters")
	public Object get(@PathVariable("node") UUID nodeId,
			@RequestParam(name = "key", required = false) String parameterPath) {
		Parameter parameter = getNode(nodeId).getParameter();

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

	private Node getNode(UUID nodeId) {
		return nodeRepository.findById(nodeId).orElseThrow(new Supplier<ResponseStatusException>() {
			@Override
			public ResponseStatusException get() {
				return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Node not found.");
			}
		});
	}

}
