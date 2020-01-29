package de.uni_jena.cs.fusion.abecto.processor.model;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.UUID;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.PatternVars;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class Category {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Category")
	public Resource category;
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
		this.validate();
	}

	public ElementGroup getPatternElementGroup() {
		// TODO cache element group
		try {
			SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(this.pattern.getBytes()));
			return (ElementGroup) parser.GroupGraphPatternSub();
		} catch (ParseException e) {
			throw new IllegalStateException("Failed to parse category pattern.", e);
		}
	}

	public Collection<Var> getPatternVariables() {
		return PatternVars.vars(this.getPatternElementGroup());
	}

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

	public void validate() throws IllegalArgumentException {
		if (!this.getPatternVariables().stream().anyMatch((v) -> v.getVarName().equals(this.name))) {
			throw new IllegalArgumentException("Template does not contain variable named \"" + this.name + "\".");
		}
	}
}
