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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;

public class ManualMappingProcessor extends AbstractMappingProcessor<ManualMappingProcessor.Parameter> {

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID ontologyId1, UUID ontologyId2)
			throws IllegalStateException, NullPointerException, IllegalArgumentException, ReflectiveOperationException {
		Collection<Mapping> mappings = new HashSet<>();
		for (Collection<String> allEquivalent : this.getParameters().mappings.orElse(Collections.emptyList())) {
			for (String entity1 : allEquivalent) {
				Resource resource1 = ResourceFactory.createResource(entity1);
				for (String entity2 : allEquivalent) {
					if (entity1.compareTo(entity2) < 0) {// save some work
						Resource resource2 = ResourceFactory.createResource(entity2);
						mappings.add(Mapping.of(resource1, resource2));
					}
				}
			}
		}
		for (Collection<String> allDifferent : this.getParameters().suppressed_mappings
				.orElse(Collections.emptyList())) {
			for (String entity1 : allDifferent) {
				Resource resource1 = ResourceFactory.createResource(entity1);
				for (String entity2 : allDifferent) {
					if (entity1.compareTo(entity2) < 0) {// save some work
						Resource resource2 = ResourceFactory.createResource(entity2);
						mappings.add(Mapping.not(resource1, resource2));
					}
				}
			}
		}
		return mappings;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Optional<Collection<Collection<String>>> mappings = Optional.empty();
		public Optional<Collection<Collection<String>>> suppressed_mappings = Optional.empty();
	}
}
