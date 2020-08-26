/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Mappings;

public class RelationalMappingProcessor extends AbstractMappingProcessor<RelationalMappingProcessor.Parameter> {

	@Override
	public Collection<Mapping> computeMapping(Model model1, Model model2, UUID ontologyId1, UUID ontologyId2)
			throws Exception {

		// load categories
		String categoryName = this.getParameters().category;
		Category category1;
		try {
			category1 = SparqlEntityManager.selectOne(new Category(categoryName, null, ontologyId1), this.metaModel)
					.orElseThrow();
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException
				| NoSuchElementException e) {
			throw new Exception("Failed to load category definition for ontology 1.", e);
		}
		Category category2;
		try {
			category2 = SparqlEntityManager.selectOne(new Category(categoryName, null, ontologyId2), this.metaModel)
					.orElseThrow();
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException
				| NoSuchElementException e) {
			throw new Exception("Failed to load category definition for ontology 2.", e);
		}

		// check variables
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

		// load mappings form ontology 1 to other ontologies
		Map<Resource, Collection<Resource>> mappedResourceOfOtherOntsByResourceOfOnt1 = new HashMap<>();
		try {
			for (Mapping mapping : Mappings.getPositiveMappings(this.metaModel)) {
				mappedResourceOfOtherOntsByResourceOfOnt1.computeIfAbsent(mapping.resource1, (v) -> {
					return new ArrayList<Resource>();
				}).add(mapping.resource2);
				mappedResourceOfOtherOntsByResourceOfOnt1.computeIfAbsent(mapping.resource2, (v) -> {
					return new ArrayList<Resource>();
				}).add(mapping.resource1);
			}
		} catch (IllegalStateException | NullPointerException | ReflectiveOperationException e) {
			throw new Exception("Failed to load existing mappings.", e);
		}

		// prepare entity index of ontology 2
		Map<String, Map<Resource, Collection<Resource>>> relationByResourceByVariable = new HashMap<>();
		for (String variable : variables) {
			relationByResourceByVariable.put(variable, new HashMap<>());
		}
		ResultSet categoryResults = category2.selectCategory(model2);
		resultLoop: while (categoryResults.hasNext()) {
			QuerySolution solution = categoryResults.next();
			Resource entity = solution.getResource(categoryName);
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						Resource value = solution.getResource(variable);
						relationByResourceByVariable.get(variable).computeIfAbsent(value, (v) -> {
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
		categoryResults = category1.selectCategory(model1);
		resultLoop: while (categoryResults.hasNext()) {
			QuerySolution solution = categoryResults.next();
			List<Set<Resource>> candidateSets = new ArrayList<>();
			Resource entity = solution.getResource(categoryName);
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						Set<Resource> candidateSet = new HashSet<>();
						Resource variableResource = solution.getResource(variable);
						for (Resource mappedResource : mappedResourceOfOtherOntsByResourceOfOnt1
								.getOrDefault(variableResource, Collections.emptySet())) {
							candidateSet.addAll(relationByResourceByVariable.get(variable).getOrDefault(mappedResource,
									Collections.emptySet()));
						}
						candidateSets.add(candidateSet);
					} catch (ClassCastException e) {
						// value is not a resource
						Issue issue = Issue.unexpectedValueType(ontologyId1, entity, variable, "resource");
						SparqlEntityManager.insert(issue, this.getResultModel());
						continue resultLoop;
					}
				} else {
					// skip resources with missing value
					continue resultLoop;
				}
			}

			// get intersection of candidateSets
			Optional<Set<Resource>> mappedEntities = candidateSets.stream().reduce((a, b) -> {
				a.retainAll(b);
				return a;
			});
			// add left candidates to mappings
			for (Resource mappedEntity : mappedEntities.orElseGet(Collections::emptySet)) {
				mappings.add(Mapping.of(entity, mappedEntity));
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
