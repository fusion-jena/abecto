package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;

public class ManualMappingProcessor extends AbstractMappingProcessor<ManualMappingProcessor.Parameter> {

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2) {
		Collection<Mapping> mappings = new HashSet<>();

		for (Collection<String> allEquivalent : this.getParameters().mappings.orElse(Collections.emptyList())) {
			for (String firstEntity : allEquivalent) {
				for (String secondEntity : allEquivalent) {
					if (!firstEntity.equals(secondEntity)) {
						mappings.add(Mapping.of(ResourceFactory.createResource(firstEntity),
								ResourceFactory.createResource(secondEntity)));
					}
				}
			}
		}
		for (Collection<String> allEquivalent : this.getParameters().suppressed_mappings
				.orElse(Collections.emptyList())) {
			for (String entity1 : allEquivalent) {
				for (String entity2 : allEquivalent) {
					if (!entity1.equals(entity2)) {
						mappings.add(Mapping.not(ResourceFactory.createResource(entity1),
								ResourceFactory.createResource(entity2)));
						// TODO set knowledge bases
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
