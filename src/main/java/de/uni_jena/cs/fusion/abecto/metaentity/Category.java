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
package de.uni_jena.cs.fusion.abecto.metaentity;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.PatternVars;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;
import de.uni_jena.cs.fusion.abecto.util.Default;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Category {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Category")
	public Resource id;
	/**
	 * The name of the category and simultaneous the name of the primary key
	 * variable.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:categoryName")
	public String name;
	/**
	 * The pattern describing the category.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:categoryPattern")
	public String pattern;
	/**
	 * The ontology this category definition belongs to.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:ontology")
	public UUID ontology;

	public Category() {
	}

	public Category(String name, String pattern, UUID ontology) throws IllegalArgumentException {
		this.name = name;
		this.pattern = pattern;
		this.ontology = ontology;
		if (pattern != null) {
			this.validate();
		}
	}

	@JsonIgnore
	public ElementGroup getPatternElementGroup() {
		// TODO cache element group (and copy in #contains())
		try {
			SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(this.pattern.getBytes()));
			parser.setPrologue(Default.PROLOGUE);
			return (ElementGroup) parser.GroupGraphPattern();
		} catch (ParseException e) {
			throw new IllegalStateException("Failed to parse category pattern.", e);
		}
	}

	@JsonIgnore
	public Collection<Var> getPatternVariables() {
		return PatternVars.vars(this.getPatternElementGroup()).stream()
				// remove helper vars for BlankNodePropertyLists/BlankNodePropertyListPaths
				.filter((var) -> !var.getVarName().startsWith("?"))//
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public Query getQuery() {
		Query query = new Query();
		query.setQuerySelectType();
		query.addResultVar(name);
		this.getPatternVariables().forEach(query::addResultVar);
		query.setQueryPattern(this.getPatternElementGroup());
		return query;
	}

	@JsonIgnore
	public Query getResourceQuery() {
		Query query = new Query();
		query.setQuerySelectType();
		query.addResultVar(name);
		query.setQueryPattern(this.getPatternElementGroup());
		return query;
	}

	public ResultSet selectCategory(Model model) {
		return QueryExecutionFactory.create(this.getQuery(), model).execSelect();
	}

	public Collection<Resource> selectCategoryResources(Model model) {
		Collection<Resource> resources = new HashSet<>();

		ResultSet resultSet = QueryExecutionFactory.create(this.getResourceQuery(), model).execSelect();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			resources.add(solution.getResource(name));
		}

		return resources;
	}

	@JsonIgnore
	public Map<Resource, Map<String, Set<Literal>>> getCategoryData(Model model) {
		ResultSet solutions = this.selectCategory(model);
		List<String> variables = solutions.getResultVars();
		variables.remove(name);
		return getResultVars(solutions, name, variables);
	}

	@JsonIgnore
	public Map<Resource, Map<String, Set<Literal>>> getCategoryData(Model model, Collection<String> variables) {
		ResultSet solutions = this.selectCategory(model);
		List<String> variablesToUse = solutions.getResultVars();
		variablesToUse.remove(name);
		variablesToUse.retainAll(variables);
		return getResultVars(solutions, name, variablesToUse);
	}

	private static Map<Resource, Map<String, Set<Literal>>> getResultVars(ResultSet solutions, String key,
			Collection<String> variables) {
		Map<Resource, Map<String, Set<Literal>>> results = new HashMap<>();
		while (solutions.hasNext()) {
			QuerySolution solution = solutions.next();
			Resource resource = solution.getResource(key);
			Map<String, Set<Literal>> result = results.computeIfAbsent(resource, (x) -> new HashMap<>());
			for (String variable : variables) {
				if (solution.contains(variable)) {
					try {
						result.computeIfAbsent(variable, (x) -> new HashSet<Literal>())
								.add(solution.getLiteral(variable));
					} catch (ClassCastException e) {
						// variable is not a literal
						// ignore
					}
				}
			}
		}
		return results;
	}

	public boolean contains(Model model, Resource resource) {
		Query query = new Query();
		query.setQueryAskType();
		ElementGroup pattern = this.getPatternElementGroup();
		pattern.addElementFilter(new ElementFilter(new ExprFactory().eq(Var.alloc(name), resource.asNode())));
		query.setQueryPattern(pattern);
		return QueryExecutionFactory.create(query, model).execAsk();
	}

	public void validate() throws IllegalArgumentException {
		if (!this.getPatternVariables().stream().anyMatch((v) -> v.getVarName().equals(this.name))) {
			throw new IllegalArgumentException("Template does not contain variable named \"" + this.name + "\".");
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() + this.pattern.hashCode() + this.ontology.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Category) {
			Category other = (Category) obj;
			return Objects.equals(this.name, other.name) && Objects.equals(this.pattern, other.pattern)
					&& Objects.equals(this.ontology, other.ontology);
		}
		return false;
	}
}
