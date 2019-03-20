package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_URI;

public class ManualMappingProcessor extends AbstractMappingProcessor {

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Map.of("mappings", new TypeLiteral<Collection<Collection<String>>>() {
		}, "anti-mappings", new TypeLiteral<Collection<Collection<String>>>() {
		});
	}

	@Override
	protected Collection<Mapping> computeMapping(Graph firstGraph, Graph secondGraph) {
		Collection<Mapping> mappings = new HashSet<>();

		for (Collection<String> allEquivalent : this
				.getOptionalProperty("mappings", new TypeLiteral<Collection<Collection<String>>>() {
				}).orElse(Collections.emptyList())) {
			for (String firstEntity : allEquivalent) {
				for (String secondEntity : allEquivalent) {
					if (!firstEntity.equals(secondEntity)) {
						mappings.add(Mapping.of((Node_URI) NodeFactory.createURI(firstEntity),
								(Node_URI) NodeFactory.createURI(secondEntity)));
					}
				}
			}
		}
		for (Collection<String> allEquivalent : this
				.getOptionalProperty("anti-mappings", new TypeLiteral<Collection<Collection<String>>>() {
				}).orElse(Collections.emptyList())) {
			for (String firstEntity : allEquivalent) {
				for (String secondEntity : allEquivalent) {
					if (!firstEntity.equals(secondEntity)) {
						mappings.add(Mapping.not((Node_URI) NodeFactory.createURI(firstEntity),
								(Node_URI) NodeFactory.createURI(secondEntity)));
					}
				}
			}
		}
		return mappings;
	}

}
