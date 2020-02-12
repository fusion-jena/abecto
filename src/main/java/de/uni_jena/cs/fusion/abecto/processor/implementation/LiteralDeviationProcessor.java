package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.ValueDeviation;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class LiteralDeviationProcessor extends AbstractMetaProcessor<LiteralDeviationProcessor.Parameter> {

	public static class Parameter implements ParameterModel {
		/**
		 * Variables to process by categories.
		 */
		Map<String, Collection<String>> variables;
	}

	@Override
	protected void computeResultModel() throws Exception {
		Set<UUID> knowledgeBaseIds = this.inputGroupModels.keySet();

		Collection<ValueDeviation> deviations = new ArrayList<>();

		// iterate knowledge base pairs
		for (UUID knowledgeBaseId1 : knowledgeBaseIds) {
			Model model1 = this.inputGroupModels.get(knowledgeBaseId1);
			for (UUID knowledgeBaseId2 : knowledgeBaseIds) {
				Model model2 = this.inputGroupModels.get(knowledgeBaseId2);
				if (knowledgeBaseId1.compareTo(knowledgeBaseId2) > 0) {
					// iterate categories
					for (String categoryName : this.getParameters().variables.keySet()) {
						Optional<Category> category1Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, knowledgeBaseId1), this.metaModel);
						Optional<Category> category2Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, knowledgeBaseId2), this.metaModel);
						if (category1Optional.isPresent() && category2Optional.isPresent()) {
							Category category1 = category1Optional.orElseThrow();
							Category category2 = category2Optional.orElseThrow();

							// load mapping
							Map<Resource, Set<Resource>> mappings = new HashMap<>();
							for (Mapping mapping : SparqlEntityManager.select(
									Arrays.asList(Mapping.of(knowledgeBaseId1, knowledgeBaseId2, categoryName),
											Mapping.of(knowledgeBaseId2, knowledgeBaseId1, categoryName)),
									this.metaModel)) {
								mappings.computeIfAbsent(mapping.getResourceOf(knowledgeBaseId1), (x) -> {
									return new HashSet<>();
								}).add(mapping.getResourceOf(knowledgeBaseId2));
							}

							Collection<String> variableNames = this.getParameters().variables.get(categoryName);

							Map<Resource, Map<String, Literal>> valuesByVariableByResource2 = new HashMap<>();
							ResultSet results2 = category2.selectCategory(model2);
							while (results2.hasNext()) {
								QuerySolution result2 = results2.next();
								Resource resoure2 = result2.getResource(categoryName);

								Map<String, Literal> valuesByVariable2 = valuesByVariableByResource2
										.computeIfAbsent(resoure2, (x) -> {
											return new HashMap<>();
										});
								// iterate variables
								for (String variableName : variableNames) {
									if (result2.contains(variableName)) {
										try {
											valuesByVariable2.put(variableName, result2.getLiteral(variableName));
										} catch (ClassCastException e) {
											Issue issue = Issue.unexpectedValueType(knowledgeBaseId2, resoure2,
													variableName, "literal");
											SparqlEntityManager.insert(issue, this.getResultModel());
										}
									}
								}
							}

							ResultSet results1 = category1.selectCategory(model1);
							while (results1.hasNext()) {
								QuerySolution result1 = results1.next();
								Resource resource1 = result1.getResource(categoryName);

								// iterate variables
								for (String variableName : variableNames) {
									if (result1.contains(variableName)) {
										try {
											Literal value1 = result1.getLiteral(variableName);
											for (Resource resource2 : mappings.getOrDefault(resource1,
													Collections.emptySet())) {
												Literal value2 = valuesByVariableByResource2.get(resource2)
														.get(variableName);
												if (!value1.sameValueAs(value2)) {
													deviations.add(new ValueDeviation(null, categoryName, variableName,
															resource1, resource2, knowledgeBaseId1, knowledgeBaseId2,
															value1.toString(), value2.toString()));
												}
											}
										} catch (ClassCastException e) {
											Issue issue = Issue.unexpectedValueType(knowledgeBaseId1, resource1,
													variableName, "literal");
											SparqlEntityManager.insert(issue, this.getResultModel());
										}

									}
								}
							}
						}
					}
				}
			}
		}

		SparqlEntityManager.insert(deviations, this.getResultModel());
	}

}
