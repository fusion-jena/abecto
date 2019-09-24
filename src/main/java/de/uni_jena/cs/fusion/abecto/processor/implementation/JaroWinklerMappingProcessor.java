package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.pattern.Pattern;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.util.Queries;
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
	private Map<ElementGroup, Collection<Var>> patterns = new HashMap<>();
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
			for (Entry<ElementGroup, Collection<Var>> entry : patterns.entrySet()) {
				ElementGroup pattern = entry.getKey();

				// get pattern variables except of category
				Collection<Var> variables = entry.getValue();
				variables.removeIf(variable -> variable.toString().equals(category));

				// add value maps for each variable
				variables.forEach((variable -> modelValues.computeIfAbsent(variable, v -> new HashMap<>())));

				// execute query of the pattern
				ResultSet results = QueryExecutionFactory
						.create(Queries.categorySelect(pattern, Var.alloc(category), variables), model).execSelect();

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
	public Collection<Mapping> computeMapping(Model firstModel, Model secondModel) throws ParseException {
		// get parameters
		boolean caseSensitive = this.getParameters().case_sensitive;
		double threshold = this.getParameters().threshold;
		Collection<String> variables = this.getParameters().variables;

		// get patterns
		Query patternSelect = Queries.patternSelect(NodeFactory.createLiteral(this.getParameters().category)).build();
		ResultSet patternResults = QueryExecutionFactory.create(patternSelect, this.metaModel).execSelect();
		while (patternResults.hasNext()) {
			ElementGroup pattern = Pattern.parse(patternResults.next().get("pattern").toString());
			Collection<Var> relevantVariables = Pattern.getVariables(pattern).stream()
					.filter((var) -> variables.contains(var.getName())).collect(Collectors.toList());
			if (!relevantVariables.isEmpty()) {
				this.patterns.put(pattern, relevantVariables);
			}
		}

		// get values
		Map<Var, Map<String, Collection<Resource>>> firstModelValues = getValues(firstModel, caseSensitive);
		Map<Var, Map<String, Collection<Resource>>> secondModelValues = getValues(secondModel, caseSensitive);

		// prepare mappings collection
		Collection<Mapping> mappings = new ArrayList<>();

		// iterate variables
		for (Var variable : firstModelValues.keySet()) {
			if (secondModelValues.containsKey(variable)) {
				Map<String, Collection<Resource>> firstModelVariableValues;
				Map<String, Collection<Resource>> secondModelVariableValues;
				// ensure first values map is larger
				if (firstModelValues.get(variable).size() >= secondModelValues.get(variable).size()) {
					firstModelVariableValues = firstModelValues.get(variable);
					secondModelVariableValues = secondModelValues.get(variable);
				} else {
					firstModelVariableValues = secondModelValues.get(variable);
					secondModelVariableValues = firstModelValues.get(variable);
				}
				// prepare JaroWinklerSimilarity instance using larger values map
				JaroWinklerSimilarity<Collection<Resource>> jws = JaroWinklerSimilarity.with(firstModelVariableValues,
						threshold);

				// iterate smaller values map
				for (String label : secondModelVariableValues.keySet()) {
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
					for (Resource resource : secondModelVariableValues.get(label)) {
						for (Resource matchingResource : matchingResources) {
							mappings.add(Mapping.of(resource, matchingResource));
						}
					}
				}
			}
		}

		return mappings;
	}
}