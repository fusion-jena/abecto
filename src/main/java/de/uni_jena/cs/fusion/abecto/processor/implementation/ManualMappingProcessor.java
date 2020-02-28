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
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class ManualMappingProcessor extends AbstractMappingProcessor<ManualMappingProcessor.Parameter> {

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID knowledgeBaseId1, UUID knowledgeBaseId2)
			throws IllegalStateException, NullPointerException, IllegalArgumentException, ReflectiveOperationException {
		Collection<Mapping> mappings = new HashSet<>();

		Collection<Category> categories1 = SparqlEntityManager.select(new Category(null, null, knowledgeBaseId1),
				this.metaModel);
		Collection<Category> categories2 = SparqlEntityManager.select(new Category(null, null, knowledgeBaseId2),
				this.metaModel);

		for (Collection<String> allEquivalent : this.getParameters().mappings.orElse(Collections.emptyList())) {
			for (String entity1 : allEquivalent) {
				Resource resource1 = ResourceFactory.createResource(entity1);
				for (Category category1 : categories1) {
					if (category1.contains(model1, resource1)) {
						for (String entity2 : allEquivalent) {
							if (!entity1.equals(entity2)) {// save some work
								Resource resource2 = ResourceFactory.createResource(entity2);
								for (Category category2 : categories2) {
									if (category1.name.equals(category2.name)
											&& category1.contains(model2, resource2)) {
										mappings.add(Mapping.of(resource1, resource2, category1.name));
									}
								}
							}
						}
					}
				}
			}
		}
		for (Collection<String> allEquivalent : this.getParameters().suppressed_mappings
				.orElse(Collections.emptyList())) {
			for (String entity1 : allEquivalent) {
				Resource resource1 = ResourceFactory.createResource(entity1);
				for (Category category1 : categories1) {
					if (category1.contains(model1, resource1)) {
						for (String entity2 : allEquivalent) {
							if (!entity1.equals(entity2)) {// save some work
								Resource resource2 = ResourceFactory.createResource(entity2);
								for (Category category2 : categories2) {
									if (category1.name.equals(category2.name)
											&& category1.contains(model2, resource2)) {
										mappings.add(Mapping.not(resource1, resource2, category1.name));
									}
								}
							}
						}
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
