package de.uni_jena.cs.fusion.abecto.processor.model;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
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

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Category {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Category")
	public Resource category;
	/**
	 * The name of the category and simultaneous the name of the primary key
	 * variable.
	 */
	@SparqlPattern(subject = "category", predicate = "abecto:categoryName")
	public String name;
	/**
	 * The pattern describing the category.
	 */
	@SparqlPattern(subject = "category", predicate = "abecto:categoryPattern")
	public String pattern;
	/**
	 * The knowledge base this category definition belongs to.
	 */
	@SparqlPattern(subject = "category", predicate = "abecto:knowledgeBase")
	public UUID knowledgeBase;

	public Category() {
	}

	public Category(String name, String pattern, UUID knowledgeBase) throws IllegalArgumentException {
		this.name = name;
		this.pattern = pattern;
		this.knowledgeBase = knowledgeBase;
		if (pattern != null) {
			this.validate();
		}
	}

	@JsonIgnore
	public ElementGroup getPatternElementGroup() {
		// TODO cache element group (and copy in #contains())
		try {
			SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(this.pattern.getBytes()));
			return (ElementGroup) parser.GroupGraphPatternSub();
		} catch (ParseException e) {
			throw new IllegalStateException("Failed to parse category pattern.", e);
		}
	}

	@JsonIgnore
	public Collection<Var> getPatternVariables() {
		return PatternVars.vars(this.getPatternElementGroup());
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

	public ResultSet selectCategory(Model model) {
		return QueryExecutionFactory.create(this.getQuery(), model).execSelect();
	}

	@JsonIgnore
	public Map<String, Map<String, Set<String>>> getCategoryData(Model model) {
		ResultSet solutions = this.selectCategory(model);
		List<String> variables = solutions.getResultVars();
		variables.remove(name);
		Map<String, Map<String, Set<String>>> results = new HashMap<>();
		while (solutions.hasNext()) {
			QuerySolution solution = solutions.next();
			String uri = solution.getResource(name).getURI();
			Map<String, Set<String>> result = results.computeIfAbsent(uri, (x) -> new HashMap<>());
			for (String variable : variables) {
				result.computeIfAbsent(variable, (x) -> new HashSet<String>()).add(solution.get(variable).toString());
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
		return this.name.hashCode() + this.pattern.hashCode() + this.knowledgeBase.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Category) {
			Category other = (Category) obj;
			return Objects.equals(this.name, other.name) && Objects.equals(this.pattern, other.pattern)
					&& Objects.equals(this.knowledgeBase, other.knowledgeBase);
		}
		return false;
	}
}
