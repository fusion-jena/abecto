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
package de.uni_jena.cs.fusion.abecto;

import static de.uni_jena.cs.fusion.abecto.Models.assertOne;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;

import com.google.common.base.Functions;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class Aspect {
	public static Aspect getAspect(Model configurationModel, Resource aspectIri) {
		String keyVariableName = Models
				.assertOne(configurationModel.listObjectsOfProperty(aspectIri, AV.keyVariableName)).asLiteral()
				.getString();

		Aspect aspect = new Aspect(aspectIri, keyVariableName);

		// add patterns
		for (Resource aspectPatter : configurationModel.listSubjectsWithProperty(AV.ofAspect, aspectIri).toList()) {
			Resource dataset = assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.associatedDataset))
					.asResource();
			Query pattern = (Query) assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.definingQuery))
					.asLiteral().getValue();
			aspect.setPattern(dataset, pattern);
		}

		return aspect;
	}

	public static Map<Resource, Aspect> getAspects(Model configurationModel) {
		// init aspcet map
		Map<Resource, Aspect> aspects = new HashMap<>();
		// get aspects
		configurationModel.listSubjectsWithProperty(RDF.type, AV.Aspect)
				.forEach(aspect -> aspects.put(aspect, getAspect(configurationModel, aspect)));
		return aspects;
	}
	public static Optional<Map<String, Set<RDFNode>>> getResource(Aspect aspect, Resource dataset, Resource keyValue,
			Model datasetModels) {
		Query query = SelectBuilder.rewrite(aspect.getPattern(dataset).cloneQuery(),
				Collections.singletonMap(aspect.getKeyVariable(), keyValue.asNode()));
		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
		if (results.hasNext()) {
			Map<String, Set<RDFNode>> values = new HashMap<>();
			for (String varName : results.getResultVars()) {
				if (!varName.equals(aspect.getKeyVariableName())) {
					values.put(varName, new HashSet<>());
				}
			}
			while (results.hasNext()) {
				QuerySolution result = results.next();
				for (Entry<String, Set<RDFNode>> entry : values.entrySet()) {
					entry.getValue().add(result.get(entry.getKey()));
				}
			}
			return Optional.of(values);
		} else {
			return Optional.empty();
		}
	}
	public static Map<String, Map<RDFNode, Set<Resource>>> getResourceIndex(Aspect aspect, Resource dataset,
			Iterable<String> variables, Model datasetModels) {
		return getResourceIndex(aspect, dataset, variables, datasetModels, Functions.identity());
	}

	public static <T> Map<String, Map<T, Set<Resource>>> getResourceIndex(Aspect aspect, Resource dataset,
			Iterable<String> variables, Model datasetModels, Function<RDFNode, T> modifier) {
		Map<String, Map<T, Set<Resource>>> index = new HashMap<>();

		Query query = aspect.getPattern(dataset).cloneQuery();
		query.resetResultVars();
		query.addResultVar(aspect.getKeyVariable());
		for (String variable : variables) {
			query.addResultVar(variable);
			index.put(variable, new HashMap<>());
		}
		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Resource keyValue = result.getResource(aspect.getKeyVariableName());
			for (String variable : variables) {
				index.get(variable).computeIfAbsent(modifier.apply(result.get(variable)), k -> new HashSet<>())
						.add(keyValue);
			}
		}
		return index;
	}

	public static Set<Resource> getResourceKeys(Aspect aspect, Resource dataset, Model datasetModels) {
		Set<Resource> resourceKeys = new HashSet<>();

		Query query = aspect.getPattern(dataset).cloneQuery();
		query.resetResultVars();
		query.addResultVar(aspect.getKeyVariable());
		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();

		String keyVariableName = aspect.getKeyVariableName();
		while (results.hasNext()) {
			resourceKeys.add(results.next().getResource(keyVariableName));
		}

		return resourceKeys;
	}

	private final Resource iri;

	private final String keyVariableName;

	private final Var keyVariable;


	private final Map<Resource, Query> patternByDataset = new HashMap<>();

	public Aspect(Resource iri, String keyVariableName) {
		this.iri = iri;
		this.keyVariableName = keyVariableName;
		this.keyVariable = Var.alloc(keyVariableName);
	}
	
	public Resource getIri() {
		return this.iri;
	}

	public Var getKeyVariable() {
		return keyVariable;
	}

	public String getKeyVariableName() {
		return keyVariableName;
	}

	public Query getPattern(Resource dataset) {
		return patternByDataset.get(dataset);
	}

	private void setPattern(Resource dataset, Query pattern) {
		patternByDataset.put(dataset, pattern);
	}
}
