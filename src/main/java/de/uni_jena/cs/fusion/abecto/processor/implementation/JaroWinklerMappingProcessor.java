package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.similarity.jarowinkler.JaroWinklerSimilarity;

public class JaroWinklerMappingProcessor extends AbstractMappingProcessor<JaroWinklerMappingProcessor.Parameter> {

	// TODO add language handling parameter

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public double threshold;
		public boolean case_sensitive;
		public String category;
		public Collection<String> variables;
	}

	/** the patterns relevant for the category mapped with contained variables */
	private Map<Category, Collection<Var>> patterns = new HashMap<>();
	/** the variable values mapped with the entities of the category by model */
	private Map<Model, Map<Var, Map<String, Collection<Resource>>>> values = new HashMap<>();

	private Map<Var, Map<String, Collection<Resource>>> getValues(Model model, boolean caseSensitive) {
		if (!this.values.containsKey(model)) {
			// do it only once for this processor instance

			// get values map for the model
			Map<Var, Map<String, Collection<Resource>>> modelValues = this.values.computeIfAbsent(model,
					m -> new HashMap<>());

			// get the processed category
			String category = this.getParameters().category;

			// iterate the patterns
			for (Entry<Category, Collection<Var>> entry : patterns.entrySet()) {
				Category pattern = entry.getKey();

				// get pattern variables except of category
				Collection<Var> variables = entry.getValue();
				variables.removeIf(variable -> variable.toString().equals(category));

				// add value maps for each variable
				variables.forEach((variable -> modelValues.computeIfAbsent(variable, v -> new HashMap<>())));

				// execute query of the pattern
				ResultSet results = pattern.selectCategory(model);

				// iterate results
				while (results.hasNext()) {
					QuerySolution result = results.next();

					// get resource of the result
					Resource entity = result.get(category).asResource();

					// iterate result variables
					for (Var variable : variables) {
						String value = result.get(variable.getVarName()).toString();

						// add result variable value to values
						modelValues.get(variable).computeIfAbsent(value, v -> new HashSet<>()).add(entity);
					}
				}

			}
		}
		return values.get(model);
	}

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID knowledgeBaseId1, UUID knowledgeBaseId2)
			throws ParseException, IllegalStateException, NullPointerException, ReflectiveOperationException {
		// get parameters
		boolean caseSensitive = this.getParameters().case_sensitive;
		double threshold = this.getParameters().threshold;
		Collection<String> variables = this.getParameters().variables;
		String categoryName = this.getParameters().category;

		// get patterns
		for (Category category : SparqlEntityManager.select(new Category(), this.metaModel)) {
			Collection<Var> relevantVariables = category.getPatternVariables().stream()
					.filter((var) -> variables.contains(var.getName())).collect(Collectors.toList());
			if (!relevantVariables.isEmpty()) {
				this.patterns.put(category, relevantVariables);
			}
		}

		// get values
		Map<Var, Map<String, Collection<Resource>>> model1Values = getValues(model1, caseSensitive);
		Map<Var, Map<String, Collection<Resource>>> model2Values = getValues(model2, caseSensitive);

		// prepare mappings collection
		Collection<Mapping> mappings = new ArrayList<>();

		// iterate variables
		for (Var variable : model1Values.keySet()) {
			if (model2Values.containsKey(variable)) {
				Map<String, Collection<Resource>> model1VariableValues;
				Map<String, Collection<Resource>> model2VariableValues;
				// ensure first values map is larger
				boolean swapped;
				if (model1Values.get(variable).size() >= model2Values.get(variable).size()) {
					swapped = false;
					model1VariableValues = model1Values.get(variable);
					model2VariableValues = model2Values.get(variable);
				} else {
					swapped = true;
					model1VariableValues = model2Values.get(variable);
					model2VariableValues = model1Values.get(variable);
				}
				// prepare JaroWinklerSimilarity instance using larger values map
				JaroWinklerSimilarity<Collection<Resource>> jws = JaroWinklerSimilarity.with(model1VariableValues,
						threshold);

				// iterate smaller values map
				for (String label : model2VariableValues.keySet()) {
					// get best matches
					Map<Collection<Resource>, Double> searchResult = jws.apply(label);
					Set<Resource> matchingResources = new HashSet<>();
					double maxSimilarity = 0d;
					for (Entry<Collection<Resource>, Double> entry : searchResult.entrySet()) {
						if (entry.getValue() > maxSimilarity) {
							matchingResources.clear();
							maxSimilarity = entry.getValue();
							matchingResources.addAll(entry.getKey());
						} else if (entry.getValue().equals(maxSimilarity)) {
							matchingResources.addAll(entry.getKey());
						} else {
							// do nothing
						}
					}
					// convert matches into mappings
					for (Resource resource : model2VariableValues.get(label)) {
						for (Resource matchingResource : matchingResources) {
							if (!swapped) {
								mappings.add(Mapping.of(matchingResource, resource, knowledgeBaseId1, knowledgeBaseId2,
										categoryName));
							} else {
								mappings.add(Mapping.of(matchingResource, resource, knowledgeBaseId2, knowledgeBaseId1,
										categoryName));
							}
						}
					}
				}
			}
		}

		return mappings;
	}
}