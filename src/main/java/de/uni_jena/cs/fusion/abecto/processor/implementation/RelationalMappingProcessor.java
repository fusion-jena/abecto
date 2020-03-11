package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Issue;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class RelationalMappingProcessor extends AbstractMappingProcessor<RelationalMappingProcessor.Parameter> {

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID ontologyId1, UUID ontologyId2)
			throws Exception {

		// load categories
		String categoryName = this.getParameters().category;
		Category category1;
		try {
			category1 = SparqlEntityManager
					.selectOne(new Category(categoryName, null, ontologyId1), this.metaModel).orElseThrow();
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException
				| NoSuchElementException e) {
			throw new Exception("Failed to load category definition for ontology 1.", e);
		}
		Category category2;
		try {
			category2 = SparqlEntityManager
					.selectOne(new Category(categoryName, null, ontologyId2), this.metaModel).orElseThrow();
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException
				| NoSuchElementException e) {
			throw new Exception("Failed to load category definition for ontology 2.", e);
		}

		// check variablesF
		Collection<String> variables = this.getParameters().variables;
		Collection<Var> category1Variables = category1.getPatternVariables();
		Collection<Var> category2Variables = category2.getPatternVariables();
		for (String variableName : variables) {
			Var variable = Var.alloc(variableName);
			if (!category1Variables.contains(variable) || !category2Variables.contains(variable)) {
				// there will be no mapping
				return Collections.emptyList();
			}
		}

		// load mappings form ontology 1 to ontology 2
		Map<Resource, Collection<Resource>> mappingIndex = new HashMap<>();
		try {
			for (Mapping mapping : SparqlEntityManager.select(Mapping.of(), this.metaModel)) {
				mappingIndex.computeIfAbsent(mapping.resource1, (v) -> {
					return new ArrayList<Resource>();
				}).add(mapping.resource2);
				mappingIndex.computeIfAbsent(mapping.resource2, (v) -> {
					return new ArrayList<Resource>();
				}).add(mapping.resource1);
			}
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException e) {
			throw new Exception("Failed to load existing mappings.", e);
		}

		// prepare entity index of ontology 2
		Map<String, Map<Resource, Collection<Resource>>> index = new HashMap<>();
		for (String variable : variables) {
			index.put(variable, new HashMap<>());
		}
		ResultSet categoryResults = category2.selectCategory(model2);
		resultLoop: while (categoryResults.hasNext()) {
			QuerySolution solution = categoryResults.next();
			Resource entity = solution.getResource(categoryName);
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						Resource value = solution.getResource(variable);
						index.get(variable).computeIfAbsent(value, (v) -> {
							return new ArrayList<Resource>();
						}).add(entity);
					} catch (ClassCastException e) {
						// value is not a resource
						Issue issue = Issue.unexpectedValueType(ontologyId2, entity, variable, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
						continue resultLoop;
					}
				}
			}
		}

		// generate mappings
		Collection<Mapping> mappings = new ArrayList<>();
		List<Collection<Resource>> candidateSets = new ArrayList<>();
		categoryResults = category1.selectCategory(model1);
		resultLoop: while (categoryResults.hasNext()) {
			QuerySolution solution = categoryResults.next();
			candidateSets.clear();
			Resource entity = solution.getResource(categoryName);
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						Resource value = solution.getResource(variable);
						for (Resource mappedValue : mappingIndex.getOrDefault(value, Collections.emptySet())) {
							candidateSets.add(index.get(variable).getOrDefault(mappedValue, Collections.emptySet()));
						}
					} catch (ClassCastException e) {
						// value is not a resource
						Issue issue = Issue.unexpectedValueType(ontologyId1, entity, variable, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
						continue resultLoop;
					}
				}
			}
			// copy first candidate set
			if (!candidateSets.isEmpty()) {
				Collection<Resource> mappedEntities = new HashSet<>(candidateSets.get(0));
				// remove candidates not present in other candidate sets
				for (int i = 1; i < candidateSets.size() && !mappedEntities.isEmpty(); i++) {
					mappedEntities.retainAll(candidateSets.get(i));
				}
				// add left candidates to mappings
				for (Resource mappedEntity : mappedEntities) {
					mappings.add(Mapping.of(entity, mappedEntity));
				}
			}
		}

		return mappings;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public String category;
		public Collection<String> variables;
	}

}
