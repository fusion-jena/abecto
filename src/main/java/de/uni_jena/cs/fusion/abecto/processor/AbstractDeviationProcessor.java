package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.ValueDeviation;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public abstract class AbstractDeviationProcessor<Parameter>
		extends AbstractMetaProcessor<AbstractDeviationProcessor.Parameter> {

	public static class Parameter implements ParameterModel {
		/**
		 * Variables to process by categories.
		 */
		public Map<String, Collection<String>> variables;
	}

	@Override
	public final void computeResultModel() throws Exception {
		Set<UUID> knowledgeBaseIds = this.inputGroupModels.keySet();

		Collection<ValueDeviation> deviations = new ArrayList<>();

		// iterate knowledge base pairs
		for (UUID knowledgeBaseId1 : knowledgeBaseIds) {
			Model model1 = this.inputGroupModels.get(knowledgeBaseId1);
			for (UUID knowledgeBaseId2 : knowledgeBaseIds) {
				if (knowledgeBaseId1.compareTo(knowledgeBaseId2) > 0) {
					Model model2 = this.inputGroupModels.get(knowledgeBaseId2);

					// load mapping
					Map<Resource, Set<Resource>> mappings = new HashMap<>();
					for (Mapping mapping : SparqlEntityManager
							.select(Arrays.asList(Mapping.of(knowledgeBaseId1, knowledgeBaseId2),
									Mapping.of(knowledgeBaseId2, knowledgeBaseId1)), this.metaModel)) {
						mappings.computeIfAbsent(mapping.getResourceOf(knowledgeBaseId1), (x) -> {
							return new HashSet<>();
						}).add(mapping.getResourceOf(knowledgeBaseId2));
					}

					// iterate categories
					for (String categoryName : this.getParameters().variables.keySet()) {
						Optional<Category> category1Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, knowledgeBaseId1), this.metaModel);
						Optional<Category> category2Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, knowledgeBaseId2), this.metaModel);
						if (category1Optional.isPresent() && category2Optional.isPresent()) {
							Category category1 = category1Optional.orElseThrow();
							Category category2 = category2Optional.orElseThrow();

							Collection<String> variableNames = this.getParameters().variables.get(categoryName);

							deviations.addAll(computeDeviations(model1, model2, knowledgeBaseId1, knowledgeBaseId2,
									categoryName, variableNames, category1, category2, mappings));
						}
					}
				}
			}
		}

		SparqlEntityManager.insert(deviations, this.getResultModel());
	}

	public abstract Collection<ValueDeviation> computeDeviations(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception;

}
