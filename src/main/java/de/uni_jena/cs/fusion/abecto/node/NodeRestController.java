/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.node;

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
public class NodeRestController {

	@Autowired
	ProcessingRunner processorRunner;
	@Autowired
	ObjectMapper JSON;

	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	OntologyRepository ontologyRepository;
	@Autowired
	NodeRepository nodeRepository;
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
	 * @param inputNodeIds
	 * @return
	 */
	@PostMapping("/node")
	public Node create(@RequestParam("class") String processorClassName,
			@RequestParam(name = "ontology", required = false) UUID ontologyId,
			@RequestParam(name = "input", required = false) Collection<UUID> inputNodeIds,
			@RequestParam(name = "parameters", required = false) String parameterJson) {

		Class<Processor<?>> processorClass = getProcessorClass(processorClassName);

		if (SourceProcessor.class.isAssignableFrom(processorClass)) {
			// check input parameter
			if (inputNodeIds != null) {
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
			// create node
			Parameter parameter = parameterRepository.save(new Parameter(getParameter(processorClass, parameterJson)));
			return nodeRepository.save(new Node(processorClass, parameter, ontology));

		} else {
			// check ontology parameter
			if (ontologyId != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Parameter \"ontology\" only permited for SourceProcessors.");
			}
			// check input parameter
			for (UUID inputNodeId : inputNodeIds) {
				if (!nodeRepository.existsById(inputNodeId)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							String.format("Input node %s not found.", inputNodeId));
				}
			}
			Iterable<Node> inputNodes = nodeRepository.findAllById(inputNodeIds);
			// create node
			Parameter parameter = parameterRepository.save(new Parameter(getParameter(processorClass, parameterJson)));
			return nodeRepository.save(new Node(processorClass, parameter, inputNodes));
		}
	}

	@GetMapping({ "/node/{uuid}" })
	public Node get(@PathVariable("uuid") UUID uuid) {
		Optional<Node> node = nodeRepository.findById(uuid);
		if (node.isPresent()) {
			return node.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Node %s not found.", uuid));
		}
	}

	@PostMapping("/node/{uuid}/load")
	public Processing load(@PathVariable("uuid") UUID nodeId,
			@RequestParam(name = "file", required = false) MultipartFile file) {
		Node node = get(nodeId);
		Processing processing = processingRepository.save(new Processing(node));
		try {
			if (file == null) {
				processorRunner.syncExecute(processing);
			} else {
				processorRunner.syncExecute(processing, file.getInputStream());
			}
		} catch (IllegalArgumentException | IllegalStateException | IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Failed to execute Processor for Processing %s.", nodeId), e);
		}
		return processing;
	}

	@GetMapping("/node/{uuid}/processing")
	public Iterable<Processing> processings(@PathVariable("uuid") UUID nodeId) {
		return processingRepository.findByNodeOrderByStartDateTime(get(nodeId));
	}

	@DeleteMapping("/node/{uuid}")
	public void delete(@PathVariable("uuid") UUID uuid) {
		Node node = this.get(uuid);
		for (Processing processing : processingRepository.findAllByNode(node)) {
			processingRestController.delete(processing.getId());
		}
		nodeRepository.delete(node);
	}

	@GetMapping("/node/{uuid}/processing/last")
	public Processing lastProcessing(@PathVariable("uuid") UUID nodeId) {
		return processingRepository.findTopByNodeOrderByStartDateTimeDesc(get(nodeId));
	}

	@GetMapping("/node")
	public Iterable<Node> list(@RequestParam(name = "project") UUID projectId) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isPresent()) {
			return nodeRepository.findAllByProject(project.get());
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
