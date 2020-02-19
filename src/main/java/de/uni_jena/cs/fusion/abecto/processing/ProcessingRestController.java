package de.uni_jena.cs.fusion.abecto.processing;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.model.ModelRepository;

@RestController
public class ProcessingRestController {
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ModelRepository modelRepository;

	@GetMapping("/processing/{uuid}")
	public Processing getProcessing(@PathVariable("uuid") UUID processingId) {
		return processingRepository.findById(processingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing not found."));
	}

	@GetMapping("/processing/{uuid}/delete")
	public void delete(@PathVariable("uuid") UUID uuid) {
		processingRepository.delete(this.getProcessing(uuid));
	}

	/**
	 * Returns the {@link Processing}s result {@link Model} as stored in the
	 * {@link ModelRepository}.
	 */
	@GetMapping("/processing/{uuid}/model")
	public void getModel(HttpServletResponse response, @PathVariable("uuid") UUID processingId) throws IOException {
		Processing processing = processingRepository.findById(processingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing not found."));
		response.setContentType(ModelRepository.RDF_SERIALIZATION_LANG.getMimeType());
		modelRepository.get(processing.getModelHash()).write(response.getOutputStream(),
				ModelRepository.RDF_SERIALIZATION_LANG.getApacheJenaKey());
	}

	/**
	 * Returns the {@link Processing}s results as JSON-LD.
	 */
	@GetMapping("/processing/{uuid}/result")
	public Model getResult(@PathVariable("uuid") UUID processingId) {
		Processing processing = processingRepository.findById(processingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing not found."));
		return modelRepository.get(processing.getModelHash());
	}
}
