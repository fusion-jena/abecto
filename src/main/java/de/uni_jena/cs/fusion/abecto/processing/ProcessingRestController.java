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
package de.uni_jena.cs.fusion.abecto.processing;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.model.ModelRepository;
import de.uni_jena.cs.fusion.abecto.model.Models;

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

	@DeleteMapping("/processing/{uuid}")
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
		response.setContentType(ModelRepository.SERIALIZATION_LANG.getContentType().getContentTypeStr());
		Models.write(response.getOutputStream(), modelRepository.get(processing.getModelHash()),
				ModelRepository.SERIALIZATION_LANG);
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
