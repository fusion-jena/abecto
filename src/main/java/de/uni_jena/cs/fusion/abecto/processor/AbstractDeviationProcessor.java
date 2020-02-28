package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
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
import de.uni_jena.cs.fusion.abecto.processor.model.Deviation;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
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

		Collection<Deviation> deviations = new ArrayList<>();

		// load mapping
		Map<Resource, Set<Resource>> mappings = new HashMap<>();
		for (Mapping mapping : SparqlEntityManager.select(Mapping.of(), this.metaModel)) {
			mappings.computeIfAbsent(mapping.resource1, (x) -> {
				return new HashSet<>();
			}).add(mapping.resource2);
			mappings.computeIfAbsent(mapping.resource2, (x) -> {
				return new HashSet<>();
			}).add(mapping.resource1);
		}

		// iterate knowledge base pairs
		for (UUID knowledgeBaseId1 : knowledgeBaseIds) {
			Model model1 = this.inputGroupModels.get(knowledgeBaseId1);
			for (UUID knowledgeBaseId2 : knowledgeBaseIds) {
				if (knowledgeBaseId1.compareTo(knowledgeBaseId2) > 0) { // do not do work twice
					Model model2 = this.inputGroupModels.get(knowledgeBaseId2);

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

	/**
	 * Computes the deviations of two models.
	 * 
	 * @param model1           the first model to process
	 * @param model2           the second model to process
	 * @param knowledgeBaseId1 the knowledge base id of the first model
	 * @param knowledgeBaseId2 the knowledge base id of the second model
	 * @param categoryName
	 * @param variableNames
	 * @param category1
	 * @param category2
	 * @param mappings         the given mappings, resources may not belong to the
	 *                         given knowledge bases
	 * @return the computed deviations
	 * @throws Exception
	 */
	public abstract Collection<Deviation> computeDeviations(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception;

}
