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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.processor.AbstractDeviationProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.processor.model.Deviation;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class ResourceDeviationProcessor extends AbstractDeviationProcessor<AbstractDeviationProcessor.Parameter> {

	@Override
	public Collection<Deviation> computeDeviations(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception {

		Collection<Deviation> deviations = new ArrayList<>();

		Map<Resource, Map<String, Resource>> valuesByVariableByResource2 = new HashMap<>();
		ResultSet results2 = category2.selectCategory(model2);
		while (results2.hasNext()) {
			QuerySolution result2 = results2.next();
			Resource resource2 = result2.getResource(categoryName);

			Map<String, Resource> valuesByVariable2 = valuesByVariableByResource2.computeIfAbsent(resource2, (x) -> {
				return new HashMap<>();
			});
			// iterate variables
			for (String variableName : variableNames) {
				if (result2.contains(variableName)) {
					try {
						valuesByVariable2.put(variableName, result2.getResource(variableName));
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(knowledgeBaseId2, resource2, variableName, "resource");
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
						Resource value1 = result1.getResource(variableName);
						for (Resource resource2 : mappings.getOrDefault(resource1, Collections.emptySet())) {
							if (valuesByVariableByResource2.containsKey(resource2)) {
								Resource value2 = valuesByVariableByResource2.get(resource2).get(variableName);
								if (value2 != null
										&& (!mappings.containsKey(value1) || !mappings.get(value1).contains(value2))) {
									deviations.add(new Deviation(null, categoryName, variableName, resource1, resource2,
											knowledgeBaseId1, knowledgeBaseId2, "<" + value1.getURI() + ">",
											"<" + value2.getURI() + ">"));
								}
							}
						}
					} catch (ClassCastException e) {
						Issue issue = Issue.unexpectedValueType(knowledgeBaseId1, resource1, variableName, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
					}
				}
			}
		}

		return deviations;
	}

}
