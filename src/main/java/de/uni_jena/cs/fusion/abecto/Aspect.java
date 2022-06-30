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
package de.uni_jena.cs.fusion.abecto;

import static de.uni_jena.cs.fusion.abecto.util.Models.assertOne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.RecursiveElementVisitor;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;
import de.uni_jena.cs.fusion.abecto.util.Values;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class Aspect {

	final static Logger log = LoggerFactory.getLogger(Aspect.class);

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
			for (Resource dataset : configurationModel.listObjectsOfProperty(aspectPatter, AV.associatedDataset)
					.mapWith(RDFNode::asResource).toList()) {
				Query pattern = (Query) assertOne(
						configurationModel.listObjectsOfProperty(aspectPatter, AV.definingQuery)).asLiteral()
						.getValue();
				if (!pattern.isSelectType()) {
					throw new IllegalArgumentException(
							String.format("Pattern of aspect %s and dataset %s is not a SPARQL Select Query.",
									aspectIri.getURI(), dataset.getURI()));
				}
				aspect.setPattern(dataset, pattern);
			}
		}

		return aspect;
	}

	/**
	 * Returns all {@link Aspect Aspects} in the given configuration {@link Model}.
	 * 
	 * @param configurationModel the configuration {@link Model} containing the
	 *                           aspect definitions
	 * @return the {@link Aspect Aspects}
	 */
	public static Collection<Aspect> getAspects(Model configurationModel) {
		// init aspect list
		Collection<Aspect> aspects = new ArrayList<>();
		// get aspects
		configurationModel.listResourcesWithProperty(RDF.type, AV.Aspect)
				.mapWith(aspect -> getAspect(configurationModel, aspect)).forEach(aspects::add);
		return aspects;
	}

	/**
	 * 
	 * @param aspect
	 * @param dataset
	 * @param keyValue
	 * @param datasetModels
	 * @return
	 * 
	 * @throws NullPointerException if no pattern is defined for the given dataset
	 */
	public static Optional<Map<String, Set<RDFNode>>> getResource(Aspect aspect, Resource dataset, Resource keyValue,
			Model datasetModels) throws NullPointerException {
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
					RDFNode value = result.get(entry.getKey());
					if (value != null) {
						entry.getValue().add(value);
					}
				}
			}
			return Optional.of(values);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Returns an index of all resources of a given {@link Aspect} and a given
	 * dataset by its variables and by the variable values. {@code null} values will
	 * be ignored.
	 * 
	 * @param aspect        the aspect describing the resources to index
	 * @param dataset       the dataset to index the resources for
	 * @param variables     the variables to use for indexing
	 * @param datasetModels the (union of) {@link Model Model(s)} containing the
	 *                      {@link Resource Resources} to index
	 * @return
	 * 
	 * @throws NullPointerException if no pattern is defined for the given dataset
	 */
	public static Map<String, Map<RDFNode, Set<Resource>>> getResourceIndex(Aspect aspect, Resource dataset,
			Collection<String> variables, Model datasetModels) throws NullPointerException {
		return getResourceIndex(aspect, dataset, variables, datasetModels, Functions.identity());
	}

	/**
	 * Returns an index of all resources of a given {@link Aspect} and a given
	 * dataset by its variables and by the variable values. {@code null} values will
	 * be ignored. The variable values will be modified by the provided
	 * {@link Function} {@code modifier}.
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
	 * 
	 * @throws NullPointerException if no pattern is defined for the given dataset
	 */
	public static <T> Map<String, Map<T, Set<Resource>>> getResourceIndex(Aspect aspect, Resource dataset,
			Collection<String> variables, Model datasetModels, Function<RDFNode, T> modifier)
			throws NullPointerException {
		Map<String, Map<T, Set<Resource>>> index = new HashMap<>();

		for (String variable : variables) {
			index.put(variable, new HashMap<>());
		}

		Query query = aspect.getPattern(dataset);

		// remove not needed variables from query
		query = retainVariables(query, aspect.keyVariable, variables);

		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Resource keyValue = result.getResource(aspect.getKeyVariableName());
			for (String variable : variables) {
				if (result.contains(variable)) {
					index.get(variable).computeIfAbsent(modifier.apply(result.get(variable)), k -> new HashSet<>())
							.add(keyValue);
				}
			}
		}
		return index;
	}

	/**
	 * Removes all result variables from a {@link Query} except of variables given
	 * in {@code keyVariable} and {@code variables}.
	 * 
	 * @param query
	 * @param keyVariable
	 * @param variables
	 * @return
	 */
	static Query retainVariables(Query query, Var keyVariable, Collection<String> variables) {
		// TODO HOTFIX for https://issues.apache.org/jira/browse/JENA-2335
		return query;
		// Op op = new AlgebraGenerator().compile(query);
		// op = new OpProject(op,
		// query.getResultVars().stream().map(Var::alloc).filter(v ->
		// v.equals(keyVariable) ||
		// variables.contains(v.getName())).collect(Collectors.toList()));
		// return OpAsQuery.asQuery(op);
	}

	/**
	 * Returns a hash index on multiple variables for {@link Resource Resources} of
	 * a given {@link Aspect}. Resources with unbound variables are omitted.
	 * 
	 * @param aspect
	 * @param dataset
	 * @param variables
	 * @param datasetModels
	 * @return
	 */
	public static Map<Values, Set<Resource>> getResourceHashIndex(Aspect aspect, Resource dataset,
			List<String> variables, Model datasetModels) {
		Map<Values, Set<Resource>> index = new HashMap<>();

		Query query = aspect.getPattern(dataset);
		List<String> resultVars = query.getResultVars();
		if (!resultVars.containsAll(variables)) { // skip if unknown variable
			log.warn("Failed to create resources hash index of aspect {} and dataset {}: Unknown variable(s): {}",
					aspect.getIri(), dataset, variables.stream().filter(v -> resultVars.contains(v)).toArray());
			return index;
		}

		// remove not needed variables from query
		query = retainVariables(query, aspect.keyVariable, variables);

		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Resource keyValue = result.getResource(aspect.getKeyVariableName());
			if (variables.stream().allMatch(var -> result.contains(var))) { // skip resources with unbound variables
				Values valueArray = new Values(
						variables.stream().map(var -> result.get(var)).toArray(l -> new RDFNode[l]));
				index.computeIfAbsent(valueArray, k -> new HashSet<>()).add(keyValue);
			}
		}
		return index;
	}

	public static Map<Resource, Map<String, Set<RDFNode>>> getResources(Aspect aspect, Resource dataset,
			List<String> variables, Model datasetModels) {
		Map<Resource, Map<String, Set<RDFNode>>> resources = new HashMap<>();

		if (aspect.patternByDataset.containsKey(dataset)) {
			Query query = aspect.getPattern(dataset);
			// remove not needed variables from query
			query = retainVariables(query, aspect.keyVariable, variables);

			ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				Resource keyValue = result.getResource(aspect.getKeyVariableName());
				Map<String, Set<RDFNode>> resourceValues = resources.computeIfAbsent(keyValue, k -> new HashMap<>());
				for (String variable : variables) {
					if (result.contains(variable)) {
						resourceValues.computeIfAbsent(variable, k -> new HashSet<>()).add(result.get(variable));
					}
				}
			}
		}
		return resources;
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
	 * 
	 * @throws NullPointerException if no pattern is defined for the given dataset
	 */
	public static Set<Resource> getResourceKeys(Aspect aspect, Resource dataset, Model datasetModels)
			throws NullPointerException {
		Set<Resource> resourceKeys = new HashSet<>();

		Query aspectQuery = aspect.getPattern(dataset);
		// close query without result vars
		Query query = new Query();
		query.setQuerySelectType();
		query.setPrefixMapping(aspectQuery.getPrefixMapping());
		query.setBase(aspectQuery.getBase());
		query.setQueryPattern(aspectQuery.getQueryPattern());
		if (aspectQuery.getValuesData() != null) {
			query.setValuesDataBlock(aspectQuery.getValuesVariables(), aspectQuery.getValuesData());
		}
		query.setLimit(aspectQuery.getLimit());
		query.setOffset(aspectQuery.getOffset());
		if (aspectQuery.getOrderBy() != null) {
			aspectQuery.getOrderBy()
					.forEach(sortCondition -> query.addOrderBy(sortCondition.expression, sortCondition.direction));
		}
		aspectQuery.getGroupBy().forEachVarExpr(query::addGroupBy);
		aspectQuery.getHavingExprs().forEach(query::addHavingCondition);
		// set var
		query.addResultVar(aspect.getKeyVariable());
		query.setDistinct(true);

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

	/**
	 * Returns the pattern for the given dataset.
	 * 
	 * @param dataset
	 * @return the pattern for the given dataset
	 * 
	 * @throws NullPointerException if no pattern is defined for the given dataset
	 */
	public Query getPattern(Resource dataset) throws NullPointerException {
		return Objects.requireNonNull(patternByDataset.get(dataset),
				() -> String.format("Pattern of aspect %s for dataset %s not defined.", this.keyVariableName, dataset));
	}

	public Set<Resource> getDatasets() {
		return new HashSet<Resource>(patternByDataset.keySet());
	}

	public boolean coversDataset(Resource dataset) {
		return patternByDataset.containsKey(dataset);
	}

	public Aspect setPattern(Resource dataset, Query pattern) {
		patternByDataset.put(dataset, pattern);
		return this;
	}

	private Map<Resource, Map<String, Path>> variablePathsByDataset = new HashMap<>();

	public Path getVarPath(Resource dataset, String variable) {
		return variablePathsByDataset.get(dataset).get(variable);
	}

	public String getVarPathAsString(Resource dataset, String variable) {
		return TypeMapper.getInstance().getTypeByClass(Path.class).unparse(this.getVarPath(dataset, variable));
	}

	/**
	 * Determines the property paths from the key variable of this {@link Aspect} to
	 * other variables for all given dataset and adds them to the given
	 * {@link Model}.
	 * 
	 * @param model the model to add the determined paths
	 */
	public void determineVarPaths(Model model) {
		RDFDatatype sparqlPropertyPathType = TypeMapper.getInstance().getTypeByClass(Path.class);
		for (Resource dataset : patternByDataset.keySet()) {
			try {
				VarPathsExtractionVisitor visitor = new VarPathsExtractionVisitor();
				Query query = this.getPattern(dataset);
				query.getQueryPattern().visit(visitor);
				// get (blank-)node of the relevant aspect pattern
				Resource aspectPattern = model.listResourcesWithProperty(AV.associatedDataset, dataset)
						.filterKeep(r -> r.hasProperty(AV.ofAspect, this.iri)).next();
				this.variablePathsByDataset.put(dataset, visitor.getPaths(keyVariable));
				for (Entry<String, Path> variablePath : this.variablePathsByDataset.get(dataset).entrySet()) {
					aspectPattern.addProperty(AV.hasVariablePath, model.createResource(AV.VariablePath)//
							.addLiteral(AV.variableName, variablePath.getKey())//
							.addProperty(AV.propertyPath, sparqlPropertyPathType.unparse(variablePath.getValue()),
									sparqlPropertyPathType));
				}
			} catch (IllegalArgumentException e) {
				log.warn(String.format(
						"Failed to determine variables paths for aspect %s (key variable \"%s\") and dataset \"%s\".",
						this.iri, this.keyVariableName, dataset), e);
			}
		}
	}

	private static class VarPathsExtractionVisitor extends RecursiveElementVisitor {

		public VarPathsExtractionVisitor() {
			super(new ElementVisitorBase());
		}

		private Map<Node, Map<Node, Path>> paths = new HashMap<>();

		private void consumeTriplePath(TriplePath triplePath) {
			Node subject = triplePath.getSubject();
			Path path = normalize(triplePath.getPath());
			Node object = triplePath.getObject();
			if ((subject.isVariable() || subject.isBlank()) && (object.isVariable() || object.isBlank())
					&& path != null) {
//				paths.computeIfAbsent(subject, k -> new HashMap<>()).compute(object,
//						(k, v) -> (v == null) ? path : new P_Alt(path, v));
				paths.computeIfAbsent(subject, k -> new HashMap<>()).merge(object, path, P_Alt::new);
//				paths.computeIfAbsent(object, k -> new HashMap<>()).compute(subject,
//						(k, v) -> (v == null) ? inverse(path) : new P_Alt(inverse(path), v));
				paths.computeIfAbsent(object, k -> new HashMap<>()).merge(subject, inverse(path), P_Alt::new);
			}
		}

		private void expandPaths() {
			for (Node from : paths.keySet()) {
				for (Node by : new ArrayList<>(paths.get(from).keySet())) { // avoid ConcurrentModificationException
					for (Node to : paths.get(by).keySet()) {
						boolean progress;
						do {
							progress = false;
							if (!from.equals(to)) {
								LinkedList<Path> direct = pathSeq2List(paths.get(from).get(by));
								direct.addAll(pathSeq2List(paths.get(by).get(to)));
								if (!paths.get(from).containsKey(to)
										|| pathSeq2List(paths.get(from).get(to)).size() > direct.size()) {
									paths.get(from).put(to, list2PathSeq(direct));
									paths.computeIfAbsent(to, k -> new HashMap<>()).put(from,
											inverse(list2PathSeq(direct)));
									progress = true;
								}
							}
						} while (progress);
					}
				}
			}
		}

		private LinkedList<Path> pathSeq2List(Path path) {
			if (path instanceof P_Seq) {
				LinkedList<Path> list = pathSeq2List(((P_Seq) path).getLeft());
				list.addAll(pathSeq2List(((P_Seq) path).getRight()));
				return list;
			} else {
				LinkedList<Path> list = new LinkedList<Path>();
				list.add(path);
				return list;
			}
		}

		private Path list2PathSeq(LinkedList<Path> list) {
			Path path = list.getLast();
			for (int i = list.size() - 2; i >= 0; i--) {
				path = new P_Seq(list.get(i), path);
			}
			return path;
		}

		private Path normalize(Path path) {
			return (path == null) ? null : list2PathSeq(pathSeq2List(path));
		}

		public Map<String, Path> getPaths(Var from) {
			expandPaths();
			Map<String, Path> pathsToTarget = new HashMap<>();
			try {
				paths.get(from.asNode()).forEach((node, path) -> {
					if (node.isVariable() && !Var.isBlankNodeVar(node)) {
						pathsToTarget.put(node.getName(), path);
					}
				});
			} catch (NullPointerException e) {
				throw new IllegalArgumentException(String.format("Variable \"%s\" not found.", from.getVarName()));
			}
			return pathsToTarget;
		}

		private Path inverse(Path path) {
			if (path instanceof P_Inverse) {
				return ((P_Inverse) path).getSubPath();
			} else if (path instanceof P_Seq) {
				return normalize(new P_Seq(inverse(((P_Seq) path).getRight()), inverse(((P_Seq) path).getLeft())));
			} else if (path instanceof P_Alt) {
				return new P_Alt(inverse(((P_Alt) path).getLeft()), inverse(((P_Alt) path).getRight()));
			} else {
				return new P_Inverse(path);
			}
		}

		@Override
		public void startElement(ElementPathBlock el) {
			el.getPattern().forEach(this::consumeTriplePath);
		}

		@Override
		public void startElement(ElementTriplesBlock el) {
			el.getPattern().forEach(triple -> this.consumeTriplePath(new TriplePath(triple)));
		}

	}
}
