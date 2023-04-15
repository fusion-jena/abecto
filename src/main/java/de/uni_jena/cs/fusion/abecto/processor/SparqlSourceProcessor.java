/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.E_NotOneOf;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.converter.StringToQueryConverter;

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
	 * Factor to reduce the {@link #chunkSize} after failed request to the source
	 * SPARQL endpoint. Default: 0.5
	 * 
	 * @see #maxRetries
	 */
	@Parameter
	private Double chunkSizeDecreaseFactor = 0.5;
	/**
	 * Factor to increase the {@link #chunkSize} after successful request to the
	 * source SPARQL endpoint until the initial value got restores. Default: 1.5
	 * 
	 * @see #maxRetries
	 */
	@Parameter
	private Double chunkSizeIncreaseFactor = 1.5;
	/**
	 * SELECT query to retrieve a list of the relevant resources. All variables will
	 * be taken into account. None IRI values will be ignored. ORDER BY, LIMIT and
	 * OFFSET might become overwritten.
	 */
	@Parameter(converter = StringToQueryConverter.class)
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
	 * Properties to track in inverse direction to compile a list of associated
	 * resources to load. That means that the subject of a statement whose property
	 * is in this list and whose object is a loaded resource will become an
	 * associated resource.
	 */
	@Parameter
	public Collection<Resource> followInverse = new ArrayList<>();
	/**
	 * Properties that represent a hierarchy. Resources associated to a loaded
	 * resource by a followUnlimited property will be loaded unlimited, but will not
	 * cause retrieval of further resources not connected by a followUnlimited
	 * property or a followInverseUnlimited property. Default: rdfs:subClassOf,
	 * rdf:first, rdf:rest
	 */
	@Parameter
	public Collection<Resource> followUnlimited = new ArrayList<>(Arrays.asList(RDFS.subClassOf, RDF.first, RDF.rest));
	/**
	 * Properties that represent a hierarchy. Resources associated to a loaded
	 * resource by the inverse of a followInverseUnlimited property will be loaded
	 * unlimited, but will not cause retrieval of further resources not connected by
	 * a followUnlimited property or a followInverseUnlimited property.
	 */
	@Parameter
	public Collection<Resource> followInverseUnlimited = new ArrayList<>();
	/**
	 * Properties to ignore in inverse direction. Statements with one of these
	 * properties will neither get loaded nor will their subjects become an
	 * associated resource.
	 */
	@Parameter
	public Collection<Resource> ignoreInverse = new ArrayList<>();
	/**
	 * Total maximum number of retries of failed request to the source SPARQL
	 * endpoint. Default: 128
	 * 
	 * @see #chunkSizeDecreaseFactor
	 */
	@Parameter
	public Integer maxRetries = 128;

	@Override
	public void run() {
		extract(this.getOutputPrimaryModel().get(), QueryExecution.service(this.service.getURI()), this.query,
				this.list,
				this.followInverse.stream().map(r -> ResourceFactory.createProperty(r.getURI()))
						.collect(Collectors.toList()),
				this.followUnlimited.stream().map(r -> ResourceFactory.createProperty(r.getURI()))
						.collect(Collectors.toList()),
				this.followInverseUnlimited.stream().map(r -> ResourceFactory.createProperty(r.getURI()))
						.collect(Collectors.toList()));

		// TODO hotfix for https://github.com/dbpedia/extraction-framework/issues/748 & https://issues.apache.org/jira/browse/JENA-2351
		Selector withIriWithNewline = new Selector() {
			@Override
			public boolean isSimple() {return false;}

			@Override
			public Resource getSubject() {return null;}

			@Override
			public Property getPredicate() {return null;}

			@Override
			public RDFNode getObject() {return null;}

			@Override
			public boolean test(Statement statement) {
				return statement.getSubject().isURIResource() && statement.getSubject().getURI().contains("\n") ||
						statement.getPredicate().getURI().contains("\n") ||
						statement.getObject().isURIResource() && statement.getObject().asResource().getURI().contains("\n");
			}
		};
		StmtIterator statements = this.getOutputPrimaryModel().get().listStatements(withIriWithNewline);
		while (statements.hasNext()) {
			log.warn("Skipped statement due to Newline (U+000A) in IRI: " + statements.next());
			statements.remove();
		}

	}

	private static ElementGroup createElementGroup(Element... elements) {
		ElementGroup group = new ElementGroup();
		for (Element element : elements) {
			if (element != null) {
				group.addElement(element);
			}
		}
		return group;
	}

	private static Query createConstructQuery(Template template, Element pattern) {
		Query query = new Query();
		query.setQueryConstructType();
		query.setConstructTemplate(template);
		query.setQueryPattern(pattern);
		return query;
	}

	private final static Var subjectVar = Var.alloc("s"), predicateVar = Var.alloc("p"), objectVar = Var.alloc("o"),
			resourceToLoadVar = Var.alloc("l");
	private final static List<Var> valueVars = Collections.singletonList(resourceToLoadVar);

	/**
	 * 
	 * <p>
	 * <strong>Implementation Notes</strong> The method uses two separate CONSTRUCT
	 * queries to load statements containing the resources as subject or as object:
	 * <ul>
	 * <li>DESCRIBE queries could not be used, as the returned set of statements
	 * depends on the SPARQL endpoint implementation. Some implementations include
	 * statements with the resource as object, some do not. Some implementations
	 * include descriptions of used properties, some do not.
	 * <li>Two separate queries were used due to terrible query performance for
	 * combined queries.
	 * <li>Use VALUES clause inside of WHERE clause, not outside, as this is
	 * <a href="https://github.com/openlink/virtuoso-opensource/issues/921">not
	 * supported by Virtuoso</a>.
	 * </ul>
	 */
	private void loadResources(QueryExecutionBuilder service, Collection<Resource> resourcesToLoad, Model resultModel,
			boolean loadInverse) {

		// initialize queries
		Query[] queriesToExecute;
		List<Binding> currentChunk = new ArrayList<>(chunkSize);
		ElementData values = new ElementData(valueVars, currentChunk);

		BasicPattern pattern = BasicPattern
				.wrap(Collections.singletonList(new Triple(resourceToLoadVar, predicateVar, objectVar)));
		Query query = createConstructQuery(new Template(pattern),
				createElementGroup(new ElementTriplesBlock(pattern), values));

		if (loadInverse) {
			BasicPattern patternInverse = BasicPattern
					.wrap(Collections.singletonList(new Triple(subjectVar, predicateVar, resourceToLoadVar)));
			ElementFilter ignoreInverseFilter = (!ignoreInverse
					.isEmpty())
							? new ElementFilter(
									new E_NotOneOf(new ExprVar(predicateVar),
											new ExprList(ignoreInverse.stream().map(p -> new NodeValueNode(p.asNode()))
													.collect(Collectors.toList()))))
							: null;
			Query queryInverse = createConstructQuery(new Template(patternInverse),
					createElementGroup(new ElementTriplesBlock(patternInverse), ignoreInverseFilter, values));

			queriesToExecute = new Query[] { query, queryInverse };
		} else {
			queriesToExecute = new Query[] { query };
		}

		// prepare iteration
		for (Query queryToExecute : queriesToExecute) {
			int currentChunkSize = this.chunkSize;
			ListIterator<Resource> resourcesToLoadIterator = new ArrayList<>(resourcesToLoad).listIterator();
			while (resourcesToLoadIterator.hasNext()) {

				// add resource to current chunk
				currentChunk.add(BindingFactory.binding(resourceToLoadVar, resourcesToLoadIterator.next().asNode()));

				// execute chunk if necessary
				if (currentChunk.size() == currentChunkSize || // chunk full or
						!resourcesToLoadIterator.hasNext()) { // last resource
					try {

						// reuse prefixes returned by the service to shorten query
						queryToExecute.setPrefixMapping(resultModel);
						log.debug(String.format("Fetching %d resources: %s", currentChunk.size(), queryToExecute));
						service.query(queryToExecute).build().execConstruct(resultModel);
						// increase chunk size if less than chunkSize
						currentChunkSize = Math.min(this.chunkSize,
								(int) (currentChunkSize * this.chunkSizeIncreaseFactor));

					} catch (Throwable e) {
						if (this.maxRetries > 0) {
							// reduce left over retries
							this.maxRetries--;
							// reduce chunk size
							currentChunkSize = Math.max(1, (int) (currentChunkSize * this.chunkSizeDecreaseFactor));
							for (int i = 0; i < currentChunk.size(); i++) {
								// redo resources of current chunk
								resourcesToLoadIterator.previous();
							}
							log.warn(
									String.format("Request failed: %s\n%s", e.getMessage(), queryToExecute.toString()));
							log.warn(String.format("Continue with reduced chunk size: %d Left retries: %s",
									currentChunkSize, this.maxRetries));
						} else {
							throw e;
						}
					} finally {
						// reset chunk
						currentChunk.clear();
					}
				}
			}
		}
	}

	private Model extract(Model resultModel, QueryExecutionBuilder service, Optional<Query> query,
			Collection<Resource> list, Collection<Property> followInverse, Collection<Property> followUnlimited,
			Collection<Property> followInverseUnlimited) {

		Set<Resource> resourcesLoaded = new HashSet<>();
		Set<Resource> resourcesToLoad = new HashSet<>();

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
			// use prefixes of given query to shorten generated queries
			resultModel.setNsPrefixes(relevantResourceQuery.getPrefixMapping());
		}

		// get list of relevant resources using parameter `list`
		resourcesToLoad.addAll(list);

		// get descriptions of relevant resources and determine associated resources to
		// load next
		for (int distance = 0; distance <= this.maxDistance; distance++) {
			// load resources in chunks
			loadResources(service, resourcesToLoad, resultModel, true);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			if ( /* there is a next iteration */ distance < this.maxDistance) {
				// get associated resources to load in next iteration
				for (Property followInverseProperty : followInverse) {
					resultModel.listSubjectsWithProperty(followInverseProperty).filterKeep(RDFNode::isURIResource)
							.filterDrop(resourcesLoaded::contains).forEachRemaining(resourcesToLoad::add);
				}
				resultModel.listObjects().filterKeep(RDFNode::isURIResource).filterDrop(resourcesLoaded::contains)
						.mapWith(RDFNode::asResource).forEachRemaining(resourcesToLoad::add);
			}
		}

		// get descriptions of upper hierarchy resources (transitively) associated to
		// earlier loaded resources by at least one of the `hierarchyProperties`
		do {
			// load resources in chunks
			loadResources(service, resourcesToLoad, resultModel, true);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new resources associated by a followUnlimited or followInverseUnlimited
			// property to load in next iteration
			for (Property followUnlimitedProperty : followUnlimited) {
				resultModel.listObjectsOfProperty(followUnlimitedProperty).filterKeep(RDFNode::isURIResource)
						.filterDrop(resourcesLoaded::contains).mapWith(RDFNode::asResource)
						.forEachRemaining(resourcesToLoad::add);
			}
			for (Property followInverseUnlimitedProperty : followInverseUnlimited) {
				resultModel.listSubjectsWithProperty(followInverseUnlimitedProperty).filterKeep(RDFNode::isURIResource)
						.filterDrop(resourcesLoaded::contains).mapWith(RDFNode::asResource)
						.forEachRemaining(resourcesToLoad::add);
			}

		} while (!resourcesToLoad.isEmpty());

		// get descriptions of properties used to describe loaded resources
		do {
			// load resources in chunks
			loadResources(service, resourcesToLoad, resultModel, false);

			// remember loaded resources
			resourcesLoaded.addAll(resourcesToLoad);
			resourcesToLoad.clear();

			// get new properties used to describe loaded resources
			resultModel.listStatements().mapWith(Statement::getPredicate).filterDrop(resourcesLoaded::contains)
					.forEachRemaining(resourcesToLoad::add);
		} while (!resourcesToLoad.isEmpty());

		log.info(String.format("About %d statements on %d resources loaded.", resultModel.size(),
				resourcesLoaded.size()));

		return resultModel;
	}
}
