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

import static de.uni_jena.cs.fusion.abecto.util.Models.assertOne;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
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

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class Aspect {

	/**
	 * Returns an {@link Aspect} determined by an given IRI in the given
	 * configuration {@link Model}.
	 * 
	 * @param configurationModel the configuration {@link Model} containing the
	 *                           aspect definitions
	 * @param aspectIri          the IRI of the {@link Aspect} to return
	 * @return the {@link Aspect}
	 * @throws NoSuchElementException  if there is no {@link Aspect} with the given
	 *                                 IRI
	 * @throws ToManyElementsException if there are multiple pattern defined for the
	 *                                 same {@link Aspect} and dataset
	 */
	public static Aspect getAspect(Model configurationModel, Resource aspectIri)
			throws NoSuchElementException, ToManyElementsException {
		String keyVariableName = Models
				.assertOne(configurationModel.listObjectsOfProperty(aspectIri, AV.keyVariableName)).asLiteral()
				.getString();

		Aspect aspect = new Aspect(aspectIri, keyVariableName);

		// add patterns
		for (Resource aspectPatter : configurationModel.listResourcesWithProperty(AV.ofAspect, aspectIri).toList()) {
			Resource dataset = assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.associatedDataset))
					.asResource();
			Query pattern = (Query) assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.definingQuery))
					.asLiteral().getValue();
			aspect.setPattern(dataset, pattern);
		}

		return aspect;
	}

	/**
	 * Returns all {@link Aspect Aspects} in the given configuration {@link Model}.
	 * 
	 * @param configurationModel the configuration {@link Model} containing the
	 *                           aspect definitions
	 * @return the {@link Aspect Aspects} by IRI
	 */
	public static Map<Resource, Aspect> getAspects(Model configurationModel) {
		// init aspcet map
		Map<Resource, Aspect> aspects = new HashMap<>();
		// get aspects
		configurationModel.listResourcesWithProperty(RDF.type, AV.Aspect)
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

	/**
	 * Returns an index of all resources of a given {@link Aspect} and a given
	 * dataset by its variables and by the variable values.
	 * 
	 * @param aspect        the aspect describing the resources to index
	 * @param dataset       the dataset to index the resources for
	 * @param variables     the variables to use for indexing
	 * @param datasetModels the (union of) {@link Model Model(s)} containing the
	 *                      {@link Resource Resources} to index
	 * @return
	 */
	public static Map<String, Map<RDFNode, Set<Resource>>> getResourceIndex(Aspect aspect, Resource dataset,
			Iterable<String> variables, Model datasetModels) {
		return getResourceIndex(aspect, dataset, variables, datasetModels, Functions.identity());
	}

	/**
	 * Returns an index of all resources of a given {@link Aspect} and a given
	 * dataset by its variables and by the variable values. The variable values will
	 * be modified by the provided {@link Function} {@code modifier}.
	 * <p>
	 * For example, the {@code modifier} could be used to convert all characters of
	 * String variable values to lowercase characters.
	 * 
	 * @param <T>           Type of the variable values after application of the
	 *                      {@code modifier}
	 * @param aspect        the aspect describing the resources to index
	 * @param dataset       the dataset to index the resources for
	 * @param variables     the variables to use for indexing
	 * @param datasetModels the (union of) {@link Model Model(s)} containing the
	 *                      {@link Resource Resources} to index
	 * @param modifier      the {@link Function} to modify the variable values
	 *                      before building up the index
	 * @return
	 */
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

	/**
	 * Returns a {@link Set} of all resource keys of a given aspect in a given
	 * dataset.
	 * 
	 * @param aspect        the {@link Aspect} the returned {@link Resource
	 *                      Resources} belong to
	 * @param dataset       the IRI of the source dataset of the {@link Resource
	 *                      Resources}
	 * @param datasetModels the (union of) {@link Model Model(s)} of the dataset
	 *                      containing the {@link Resource Resources}
	 * @return all resource keys of the given aspect in the given dataset
	 */
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

	public void setPattern(Resource dataset, Query pattern) {
		patternByDataset.put(dataset, pattern);
	}
}
