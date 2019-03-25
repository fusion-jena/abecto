package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

public class ManualMappingProcessor extends AbstractMappingProcessor {

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Map.of("mappings", new TypeLiteral<Collection<Collection<String>>>() {}, "suppressed-mappings",
				new TypeLiteral<Collection<Collection<String>>>() {});
	}

	@Override
	protected Collection<Mapping> computeMapping(Model firstModel, Model secondModel) {
		Collection<Mapping> mappings = new HashSet<>();

		for (Collection<String> allEquivalent : this
				.getOptionalProperty("mappings", new TypeLiteral<Collection<Collection<String>>>() {})
				.orElse(Collections.emptyList())) {
			for (String firstEntity : allEquivalent) {
				for (String secondEntity : allEquivalent) {
					if (!firstEntity.equals(secondEntity)) {
						mappings.add(Mapping.of(ResourceFactory.createResource(firstEntity),
								ResourceFactory.createResource(secondEntity)));
					}
				}
			}
		}
		for (Collection<String> allEquivalent : this
				.getOptionalProperty("suppressed-mappings", new TypeLiteral<Collection<Collection<String>>>() {})
				.orElse(Collections.emptyList())) {
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
