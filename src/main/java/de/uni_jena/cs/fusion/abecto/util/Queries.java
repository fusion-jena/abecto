package de.uni_jena.cs.fusion.abecto.util;

import java.util.Collection;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;

/**
 * Provides prepared queries for meta model patterns.
 */
public class Queries {
	/**
	 * Returns a prepared select query to get pairs of category and pattern.
	 * 
	 * @param resources the resources whose mappings should be obtained, or
	 *                  {@code null} for all mappings
	 * @return the mappings
	 */
	public static SelectBuilder patternMapping(Iterable<Node> resources) {
		Node first = NodeFactory.createVariable("first");
		Node second = NodeFactory.createVariable("second");
		SelectBuilder select = new SelectBuilder().addVar(first).addVar(second).addWhere(first, Vocabulary.MAPPING,
				second);
		if (resources != null) {
			select.addValueVar(first);
			resources.forEach(select::addValueRow);
		}
		return select;
	}

	/**
	 * Returns a prepared select query to get pairs of category and pattern.
	 * 
	 * @param category the category whose patterns should be obtained, or
	 *                 {@code null} for all patterns
	 * @return the pattern select query
	 */
	public static SelectBuilder patternSelect(Node category) {
		Node blankNodeVar = NodeFactory.createVariable("category");
		Node categoryVar = NodeFactory.createVariable("name");
		Node patternVar = NodeFactory.createVariable("pattern");
		SelectBuilder select = new SelectBuilder().addVar(categoryVar).addVar(patternVar)
				.addWhere(blankNodeVar, RDF.type, Vocabulary.CATEGORY)
				.addWhere(blankNodeVar, Vocabulary.CATEGORY_NAME, categoryVar)
				.addWhere(blankNodeVar, Vocabulary.CATEGORY_PATTERN, patternVar);
		if (category != null) {
			select.setVar(categoryVar, category);
		}
		return select;
	}

	/**
	 * Returns a prepared construct query to generate a representation of pairs of
	 * category and pattern.
	 * 
	 * Use {@link ConstructBuilder#addValueRow} to add actual value pairs of
	 * category and pattern.
	 * 
	 * @return the pattern construct query
	 */
	public static ConstructBuilder patternConstruct() {
		Node blankNodeVar = NodeFactory.createBlankNode();
		Node categoryVar = NodeFactory.createVariable("name");
		Node patternVar = NodeFactory.createVariable("pattern");
		return new ConstructBuilder().addConstruct(blankNodeVar, RDF.type, Vocabulary.CATEGORY)
				.addConstruct(blankNodeVar, Vocabulary.CATEGORY_NAME, categoryVar)
				.addConstruct(blankNodeVar, Vocabulary.CATEGORY_PATTERN, patternVar).addValueVar(categoryVar)
				.addValueVar(patternVar);
	}

	public static Query categorySelect(ElementGroup pattern, Var category, Collection<Var> variables) {
		Query query = new Query();
		query.setQuerySelectType();
		query.addResultVar(category);
		variables.forEach(query::addResultVar);
		query.setQueryPattern(pattern);
		return query;
	}
}
