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
package de.uni_jena.cs.fusion.abecto.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.exec.http.QuerySendMode;
import org.apache.jena.sparql.expr.E_Datatype;
import org.apache.jena.sparql.expr.E_Lang;
import org.apache.jena.sparql.expr.E_LangMatches;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_NotOneOf;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Parameter;

public class SparqlSourceProcessor extends Processor<SparqlSourceProcessor> {

	final static Logger log = LoggerFactory.getLogger(SparqlSourceProcessor.class);

	/** URL of the SPARQL endpoint to use. */
	@Parameter
	public Resource service;
	/**
	 * Maximum number of resources to retrieve in one request. Default: 500
	 */
	@Parameter
	public Integer chunkSize = 500;
	/**
	 * SELECT query to retrieve a list of the relevant resources. All variables will
	 * be taken into account. None IRI values will be ignored. ORDER BY, LIMIT and
	 * OFFSET might become overwritten.
	 */
	@Parameter
	public Optional<Query> query = Optional.empty();
	/** List of the relevant resources. */
	@Parameter
	public Collection<Resource> list = new ArrayList<>();
	/**
	 * Maximum distance of loaded associated resources. Associated resources share a
	 * statement as an object with a retrieved resource as a subject, in case of any
	 * property, and vice versa, in case of followed inverse properties (see
	 * {@link #followInverse}). Default: 0
	 */
	@Parameter
	public Integer maxDistance = 0;
	/**
	 * Properties that represent a hierarchy. Resources associated to a loaded
	 * resource by a hierarchy property will be loaded unlimited, but will not cause
	 * retrieval of further resources not connected by a hierarchy property.
	 * Default: rdfs:subClassOf
	 */
	@Parameter
	public Collection<Resource> followUnlimited = new ArrayList<>(Arrays.asList(RDFS.subClassOf));
	/**
	 * Properties to track in inverse direction to compile a list of associated
	 * resources to load. That means that the subject of a statement whose property
	 * is in this list and whose object is a loaded resource will become a
	 * associated resource.
	 */
	@Parameter
	public Collection<Resource> followInverse = new ArrayList<>();

	/**
	 * Language patterns to filter returned literals. If not empty, only string
	 * literals will be loaded, that match at least on of these patterns. String
	 * literals without language tag will match with "", all string literals with
	 * language tag match with "*". Default: empty
	 */
	@Parameter
	public Collection<String> languageFilterPatterns = new ArrayList<>();

	// TODO add parameter to {@code followInverseUnlimited}

	@Override
	public void run() {
		// TODO remove workaround for https://issues.apache.org/jira/browse/JENA-2257
		extract(this.getOutputPrimaryModel().get(),
				QueryExecution.service(this.service.getURI()).sendMode(QuerySendMode.asPost), this.query, this.list,
				this.followInverse.stream().map(r -> ResourceFactory.createProperty(r.getURI()))
						.collect(Collectors.toList()),
				this.followUnlimited.stream().map(r -> ResourceFactory.createProperty(r.getURI()))
						.collect(Collectors.toList()),
				this.maxDistance, this.chunkSize);
	}

	private static ElementData valuesClause(Var var, Iterable<? extends Resource> values) {
		ElementData elementData = new ElementData();
		elementData.add(var);
		for (Resource value : values) {
			elementData.add(BindingFactory.binding(var, value.asNode()));
		}
		return elementData;
	}

	private final static Var s = Var.alloc("s"), p = Var.alloc("p"), o = Var.alloc("o");
	private ElementFilter languageFilter;

	private static ElementFilter languageFilter(Collection<String> languageFilterPatterns) {
		if (!languageFilterPatterns.isEmpty()) {
			ExprVar exprVar​ = new ExprVar(o);

			Expr expr = new E_NotOneOf(new E_Datatype(exprVar​), new ExprList(Arrays
					.asList(NodeValue.makeNode(RDF.langString.asNode()), NodeValue.makeNode(XSD.xstring.asNode()))));
			for (String languageFilterPattern : languageFilterPatterns) {
				expr = new E_LogicalOr(expr,
						new E_LangMatches(new E_Lang(exprVar​), new NodeValueString(languageFilterPattern)));
			}
			return new ElementFilter(expr);
		} else {
			return null;
		}
	}

	private static ElementGroup group(Element... elements) {
		ElementGroup group = new ElementGroup();
		for (Element element : elements) {
			if (element != null) {
				group.addElement(element);
			}
		}
		return group;
	}

	private void loadResources(QueryExecutionBuilder service, Collection<Resource> resourcesToLoad,
			Iterable<Property> followInverse, Model resultModel, int chunkSize) {

		// prepare queries
		/*
		 * This requires some complicated use of {@link Element}s, as both APIs, {@link
		 * Query} and {@link org.apache.jena.arq.querybuilder.ConstructBuilder} do not
		 * directly support multiple values clauses.
		 */
		BasicPattern pattern = BasicPattern.wrap(Collections.singletonList(new Triple(s, p, o)));
		ElementTriplesBlock triple = new ElementTriplesBlock(pattern);
		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		constructQuery.setConstructTemplate(new Template(pattern));
		ElementData followInverseValuesClause = valuesClause(p, followInverse);

		// initialize chunk
		List<Resource> currentChunck = new ArrayList<Resource>(chunkSize);

		Iterator<Resource> resourcesToLoadIterator = resourcesToLoad.iterator();
		while (resourcesToLoadIterator.hasNext()) {
			// add resource to query
			currentChunck.add(resourcesToLoadIterator.next());
			if (currentChunck.size() == chunkSize || // chunk completed or
					!resourcesToLoadIterator.hasNext()) { // last resource

				constructQuery.setQueryPattern(group(triple, valuesClause(s, currentChunck), languageFilter));
				// create prefixes for namespaces to shorten queries
				constructQuery.setPrefixMapping(shortPrefixMapping(currentChunck));

				log.debug(String.format("Fetching %d resources: %s", currentChunck.size(), currentChunck));
				service.query(constructQuery).build().execConstruct(resultModel);

				if (followInverse.iterator().hasNext()) {
					// add resource list as subject
					constructQuery.setQueryPattern(
							group(triple, followInverseValuesClause, valuesClause(o, currentChunck), languageFilter));

					service.query(constructQuery).build().execConstruct(resultModel);
				}
				// reset chunk
				currentChunck.clear();
			}
		}
	}

	private static String guessNamespace(Resource resource) {
		String uri = resource.getURI();
		int namespaceEndIndex = uri.endsWith("/") ? uri.lastIndexOf("/", uri.length() - 2) : uri.lastIndexOf("/");
		return uri.substring(0, namespaceEndIndex + 1);
	}

	private static PrefixMapping shortPrefixMapping(Collection<Resource> resources) {
		PrefixMapping prefixMapping = PrefixMapping.Factory.create();

		resources.stream().map(SparqlSourceProcessor::guessNamespace).forEach(namespace -> {
			if (prefixMapping.getNsURIPrefix(namespace) == null) {
				int i = prefixMapping.numPrefixes();
				String prefix = "";
				if (i > 0) {
					prefix = (char) ((i - 1) % 26 + 97)
							+ (((i - 1) / 26 > 0) ? Integer.toString((i - 1) / 26 - 1, Character.MAX_RADIX) : "");
				}
				prefixMapping.setNsPrefix(prefix, namespace);
			}
		});

		return prefixMapping;
	}

	private Model extract(Model resultModel, QueryExecutionBuilder service, Optional<Query> query,
			Collection<Resource> list, Collection<Property> followInverse, Collection<Property> followUnlimited,
			int maxDistance, int chunkSize) {

		languageFilter = languageFilter(languageFilterPatterns);

		Set<Resource> resourcesLoaded = new HashSet<Resource>();
		Set<Resource> resourcesToLoad = new HashSet<Resource>();

		// get list of relevant resources using parameter `query`
		if (query.isPresent()) {
			Query relevantResourceQuery = query.get();
			ResultSet results = service.query(relevantResourceQuery).select();
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

		// get descriptions of relevant resources and determine associated resources to
		// load next
		for (int distance = 0; distance <= maxDistance; distance++) {
			// load resources in chunks
			loadResources(service, resourcesToLoad, followInverse, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			if ( /* there is a next iteration */ distance < maxDistance) {
				// get associated resources to load in next iteration
				resultModel.listSubjects().filterKeep(RDFNode::isURIResource).filterDrop(resourcesLoaded::contains)
						.forEachRemaining(resourcesToLoad::add);
				resultModel.listObjects().filterKeep(RDFNode::isURIResource).filterDrop(resourcesLoaded::contains)
						.mapWith(RDFNode::asResource).forEachRemaining(resourcesToLoad::add);
			}
		}

		// get descriptions of upper hierarchy resources (transitively) associated to
		// earlier loaded resources by at least one of the `hierarchyProperties`
		do {
			// load resources in chunks
			loadResources(service, resourcesToLoad, followInverse, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new resources associated by a hierarchyProperty to load in next iteration
			for (Property hierarchyProperty : followUnlimited) {
				resultModel.listObjectsOfProperty(hierarchyProperty).filterKeep(RDFNode::isURIResource)
						.filterDrop(resourcesLoaded::contains).mapWith(RDFNode::asResource)
						.forEachRemaining(resourcesToLoad::add);
			}
		} while (!resourcesToLoad.isEmpty());

		// get descriptions of properties used to describe loaded resources
		do {
			// load resources in chunks
			loadResources(service, resourcesToLoad, followInverse, resultModel, chunkSize);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new properties used to describe loaded resources
			resultModel.listStatements().mapWith(Statement::getPredicate).filterDrop(resourcesLoaded::contains)
					.forEachRemaining(resourcesToLoad::add);
		} while (!resourcesToLoad.isEmpty());

		return resultModel;
	}
}
