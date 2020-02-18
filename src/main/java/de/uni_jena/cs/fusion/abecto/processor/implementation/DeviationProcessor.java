package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.processor.AbstractDeviationProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.ValueDeviation;

public class DeviationProcessor extends AbstractDeviationProcessor<AbstractDeviationProcessor.Parameter> {

	@Override
	public Collection<ValueDeviation> computeDeviations(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception {

		Collection<ValueDeviation> deviations = new ArrayList<>();

		Map<Resource, Map<String, RDFNode>> valuesByVariableByResource2 = new HashMap<>();
		ResultSet results2 = category2.selectCategory(model2);
		while (results2.hasNext()) {
			QuerySolution result2 = results2.next();
			Resource resoure2 = result2.getResource(categoryName);

			Map<String, RDFNode> valuesByVariable2 = valuesByVariableByResource2.computeIfAbsent(resoure2, (x) -> {
				return new HashMap<>();
			});
			// iterate variables
			for (String variableName : variableNames) {
				if (result2.contains(variableName)) {
					valuesByVariable2.put(variableName, result2.get(variableName));
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
					for (Resource resource2 : mappings.getOrDefault(resource1, Collections.emptySet())) {
						RDFNode node1 = result1.get(variableName);
						RDFNode node2 = valuesByVariableByResource2.get(resource2).get(variableName);
						if (node2 != null) {
							if (node1.isLiteral() && node2.isLiteral()) {
								Literal value1 = node1.asLiteral();
								Literal value2 = node2.asLiteral();
								if (value2 != null && !value1.sameValueAs(value2)) {
									deviations.add(new ValueDeviation(null, categoryName, variableName, resource1,
											resource2, knowledgeBaseId1, knowledgeBaseId2, value1.toString(),
											value2.toString()));
								}
							} else if (node1.isResource() && node2.isResource()) {
								Resource value1 = node1.asResource();
								Resource value2 = node2.asResource();
								if (!mappings.containsKey(value1) || !mappings.get(value1).contains(value2)) {
									deviations.add(new ValueDeviation(null, categoryName, variableName, resource1,
											resource2, knowledgeBaseId1, knowledgeBaseId2,
											"<" + value1.toString() + ">", "<" + value2.toString() + ">"));
									// TODO format values
								}
							} else {
								deviations.add(new ValueDeviation(null, categoryName, variableName, resource1,
										resource2, knowledgeBaseId1, knowledgeBaseId2, node1.toString(),
										node2.toString()));
							}
						}
					}
				}
			}
		}

		return deviations;
	}

}
