package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_NAME;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_PATTERN;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.pattern.Pattern;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;

public class ManualPatternProcessor extends AbstractMetaProcessor<ManualPatternProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		Map<String, Collection<String>> patterns;
	}

	@Override
	protected Model computeResultModel() throws Exception {
		Node blankNodeVar = NodeFactory.createBlankNode();
		Node categoryVar = NodeFactory.createVariable("category");
		Node patternVar = NodeFactory.createVariable("pattern");
		ConstructBuilder resultQueryBuilder = new ConstructBuilder().addConstruct(blankNodeVar, RDF.type, CATEGORY)
				.addConstruct(blankNodeVar, CATEGORY_NAME, categoryVar)
				.addConstruct(blankNodeVar, CATEGORY_PATTERN, patternVar).addValueVar(categoryVar)
				.addValueVar(patternVar);

		if (this.getParameters().patterns.isEmpty()) {
			throw new IllegalArgumentException("Empty category list.");
		}

		for (Entry<String, Collection<String>> entry : this.getParameters().patterns.entrySet()) {
			String category = entry.getKey();
			if (entry.getValue().isEmpty()) {
				throw new IllegalArgumentException("Empty pattern list.");
			}
			for (String pattern : entry.getValue()) {
				Pattern.validate(category, pattern);
				resultQueryBuilder.addValueRow(ResourceFactory.createStringLiteral(category),
						ResourceFactory.createStringLiteral(pattern));
			}
		}

		Query resultQuery = resultQueryBuilder.build();
		Model resultModel = Models.getEmptyOntModel();
		return QueryExecutionFactory.create(resultQuery, resultModel).execConstruct(resultModel);
	}

}
