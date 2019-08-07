package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import de.uni_jena.cs.fusion.abecto.processor.api.AbstractMappingProcessor;

public class ManualMappingProcessor extends AbstractMappingProcessor<ManualMappingProcessorParameter> {

	@Override
	public Collection<Mapping> computeMapping(Model firstModel, Model secondModel) {
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
		for (Collection<String> allEquivalent : this.getParameters().suppressed_mappings.orElse(Collections.emptyList())) {
			for (String firstEntity : allEquivalent) {
				for (String secondEntity : allEquivalent) {
					if (!firstEntity.equals(secondEntity)) {
						mappings.add(Mapping.not(ResourceFactory.createResource(firstEntity),
								ResourceFactory.createResource(secondEntity)));
					}
				}
			}
		}
		return mappings;
	}

}
