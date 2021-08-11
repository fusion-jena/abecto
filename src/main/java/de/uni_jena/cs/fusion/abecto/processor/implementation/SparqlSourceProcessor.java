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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.vocabulary.RDFS;

import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.processor.Processor;

public class SparqlSourceProcessor extends Processor {
	/** URL of the SPARQL endpoint to use. */
	@Parameter
	public String service;
	/**
	 * Maximum number of resources to retrieve in one request. Default: 500
	 */
	@Parameter
	public Integer chunkSize = 500;
	/**
	 * SELECT query to retrieve a list of the relevant resources. All variables will
	 * be taken into account. None IRI value will be ignored. ORDER BY, LIMIT and
	 * OFFSET might become overwritten.
	 */
	@Parameter
	public Optional<Query> query;
	/** List of the relevant resources. */
	@Parameter
	public Collection<Resource> list;
	/**
	 * Number of iterations to load associated resources, which share a statement as
	 * an object with a retrieved resource as a subject. Default: 0
	 */
	@Parameter
	public Integer associatedLoadIterations = 0;
	/**
	 * Properties that represent a hierarchy. Resources associated to a loaded
	 * resource by a hierarchy property will be loaded unlimited, but will not cause
	 * retrieval of further resources not connected by a hierarchy property.
	 * Default: rdfs:subClassOf
	 */
	@Parameter
	public Collection<Property> followUnlimited = new ArrayList<>(Arrays.asList(RDFS.subClassOf));
	/**
	 * Properties to track in inverse direction to compile a list of associated
	 * resources to load. That means that the subject of a statement whose property
	 * is in this list and whose object is a loaded resource will become a
	 * associated resource.
	 */
	@Parameter
	public Collection<Node> followInverse;
	
	// TODO add parameter to {@code followInverseUnlimited}

	@Override
	public void run() {
		extract(this.getOutputPrimaryModel().get(), this.service, this.query, this.list, this.followInverse,
				this.followUnlimited, this.associatedLoadIterations, this.chunkSize);
	}

	private static ElementData valuesClause(Var var, Iterable<Node> values) {
		ElementData elementData = new ElementData();
		elementData.add(var);
		for (Node node : values) {
			elementData.add(BindingFactory.binding(var, node));
		}
		return elementData;
	}

	private static ElementGroup group(Element... elements) {
		ElementGroup group = new ElementGroup();
		for (Element element : elements) {
			group.addElement(element);
		}
		return group;
	}

	private static void loadInto(String service, Collection<Resource> resourcesToLoad,
			Iterable<Node> inverseAssociationProperties, Model resultModel, int chunkSize) {
		// initialization

		// prepare queries
		/*
		 * This requires some complicated use of {@link Element}s, as both APIs, {@link
		 * Query} and {@link org.apache.jena.arq.querybuilder.ConstructBuilder} do not
		 * directly support multiple values clauses.
		 */
		Var s = Var.alloc("s"), p = Var.alloc("p"), o = Var.alloc("o");
		BasicPattern pattern = BasicPattern.wrap(Collections.singletonList(new Triple(s, p, o)));
		ElementTriplesBlock triple = new ElementTriplesBlock(pattern);
		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		constructQuery.setConstructTemplate(new Template(pattern));
		ElementData inverseAssiciationPropertiesValuesClause = valuesClause(p, inverseAssociationProperties);

		// initialize chunk
		List<Node> currentChunck = new ArrayList<Node>(chunkSize);

		Iterator<Resource> resourcesToLoadIterator = resourcesToLoad.iterator();
		while (resourcesToLoadIterator.hasNext()) {
			// add resource to query
			currentChunck.add(resourcesToLoadIterator.next().asNode());
			if (currentChunck.size() == chunkSize || // chunk completed or
					!resourcesToLoadIterator.hasNext()) { // last resource

				// add resource list as subject
				constructQuery.setQueryPattern(group(triple, valuesClause(s, currentChunck)));

				QueryExecutionFactory.sparqlService(service, constructQuery).execConstruct(resultModel);

				if (inverseAssociationProperties.iterator().hasNext()) {
					// add resource list as subject
					constructQuery.setQueryPattern(
							group(triple, inverseAssiciationPropertiesValuesClause, valuesClause(o, currentChunck)));

					QueryExecutionFactory.sparqlService(service, constructQuery).execConstruct(resultModel);
				}
				// reset chunk
				currentChunck.clear();
			}
		}

	}

	private static Model extract(Model resultModel, String service, Optional<Query> query, Collection<Resource> list,
			Collection<Node> inverseAssociationProperties, Collection<Property> hierarchyProperties,
			int associatedLoadIterations, int chunkSize) {

		// TODO provide single HTTP client for all requests

		Set<Resource> resourcesLoaded = new HashSet<Resource>();
		Set<Resource> resourcesToLoad = new HashSet<Resource>();

		// get list of relevant resources using parameter `query`
		if (query.isPresent()) {
			Query relevantResourceQuery = query.get();
			ResultSet results = QueryExecutionFactory.sparqlService(service, relevantResourceQuery).execSelect();
			while (results.hasNext()) {
				Binding binding = results.nextBinding();
				Iterator<Var> varIterator = binding.vars();
				while (varIterator.hasNext()) {
					Node node = binding.get(varIterator.next());
					if (node != null && node.isURI()) {
						resourcesToLoad.add(ResourceFactory.createResource(node.getURI()));
					}
				}
			}
		}

		// get list of relevant resources using parameter `list`
		resourcesToLoad.addAll(list);

		// get descriptions of relevant and associated resources
		for (int associatedLoadIteration = 0; associatedLoadIteration <= associatedLoadIterations; associatedLoadIteration++) {
			// load resources in chunks of size `chunkSize`
			loadInto(service, resourcesToLoad, inverseAssociationProperties, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			if ( /* there is a next iteration */ associatedLoadIteration < associatedLoadIterations) {
				// get associated resources to load in next iteration
				resultModel.listSubjects().filterKeep(o -> o.isURIResource()).filterDrop(resourcesLoaded::contains)
						.forEachRemaining(resourcesToLoad::add);
				resultModel.listObjects().filterKeep(o -> o.isURIResource()).filterDrop(resourcesLoaded::contains)
						.mapWith(o -> o.asResource()).forEachRemaining(resourcesToLoad::add);
			}
		}

		// get descriptions of upper hierarchy resources (transitively) associated to
		// earlier loaded resources by at least one of the `hierarchyProperties`
		do {
			// load resources in chunks of size `chunkSize`
			loadInto(service, resourcesToLoad, inverseAssociationProperties, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new resources associated by a hierarchyProperty to load in next iteration
			for (Property hierarchyProperty : hierarchyProperties) {
				resultModel.listObjectsOfProperty(hierarchyProperty).filterKeep(o -> o.isURIResource())
						.filterDrop(resourcesLoaded::contains).mapWith(o -> o.asResource())
						.forEachRemaining(resourcesToLoad::add);
			}
		} while (!resourcesToLoad.isEmpty());

		// get descriptions of properties used to describe loaded resources
		do {
			// load resources in chunks of size `chunkSize`
			loadInto(service, resourcesToLoad, inverseAssociationProperties, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new properties used to describe loaded resources
			resultModel.listStatements().mapWith(s -> s.getPredicate()).filterDrop(resourcesLoaded::contains)
					.forEachRemaining(resourcesToLoad::add);
		} while (!resourcesToLoad.isEmpty());

		return resultModel;
	}
}
