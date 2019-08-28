package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_NAME;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_TARGET;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_TEMPLATE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.COUNT_MEASURE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.KNOWLEDGE_BASE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.ModelUtils;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.pattern.Pattern;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;

public class PatternCountProcessor
		extends AbstractMetaProcessor<PatternCountProcessor.Parameter> {

	private static Query countQuery(String category, ElementGroup templatePattern, String target) {
		Query query = new SelectBuilder()
				.addVar(new ExprAggregator(new ExprVar("count").asVar(), AggregatorFactory.createCount(true)))
				.addGroupBy(new ExprVar(category)).addGroupBy(new ExprVar(target)).build();
		query.setQueryPattern(templatePattern);
		return query;
	}

	@Override
	protected Model computeResultModel() throws Exception {
		// get templates
		Query templateQuery = QueryFactory.create("SELECT ?name ?template WHERE {[ <" + RDF.type + "> <" + CATEGORY
				+ "> ; <" + CATEGORY_NAME + "> ?name; <" + CATEGORY_TEMPLATE + "> ?template; ]}");
		ResultSet templateQueryResult = QueryExecutionFactory.create(templateQuery).execSelect();

		/**
		 * Counts by knowledge base, category and target. The null target key represents
		 * the count of entities.
		 */
		Map<UUID, Map<String, Map<String, Long>>> counts = new HashMap<>();

		while (templateQueryResult.hasNext()) {
			// get template data
			QuerySolution templateQuerySolution = templateQueryResult.next();
			String category = templateQuerySolution.get("name").toString();
			String template = templateQuerySolution.get("template").toString();
			ElementGroup templatePattern = Pattern.parse(template);

			Collection<String> targets = new HashSet<>();
			Pattern.getVariables(templatePattern).forEach((v) -> targets.add(v.getName()));
			targets.remove(category);
			targets.add(null);

			// create queries
			Map<String, Query> categoryQueries = new HashMap<>();
			targets.forEach((target) -> categoryQueries.put(target, countQuery(category, templatePattern, target)));

			for (Entry<UUID, Model> entry : this.inputGroupModels.entrySet()) {
				// get control variables
				UUID knowledgeBase = entry.getKey();
				Model inputGroupModel = entry.getValue();
				// get result map
				Map<String, Long> countsByTarget = counts.computeIfAbsent(knowledgeBase, (kb) -> new HashMap<>())
						.computeIfAbsent(category, (c) -> new HashMap<>());

				for (String target : targets) {
					// get counts for current template, model and target
					Long count = QueryExecutionFactory.create(categoryQueries.get(target), inputGroupModel).execSelect()
							.next().getLiteral("count").getLong();
					// increase total count
					countsByTarget.compute(target, (k, v) -> (v == null) ? count : v + count);
				}
			}
		}

		// prepare result query
		ParameterizedSparqlString preparedQuery = new ParameterizedSparqlString();
		preparedQuery.setCommandText("CONSTRUCT {[ <" + RDF.type + "> <" + COUNT_MEASURE + "> ; <" + KNOWLEDGE_BASE
				+ "> ?knowledgebase ; <" + CATEGORY_NAME + "> ?category; <" + CATEGORY_TARGET + "> ?target; <" + VALUE
				+ "> ?count ; ]} WHERE {VALUES (?knowledgebase ?category ?target ?count) {?results} }");

		// bind results
		List<List<? extends RDFNode>> results = new ArrayList<>();
		for (Entry<UUID, Map<String, Map<String, Long>>> knowledgeBaseEntry : counts.entrySet()) {
			UUID knowledgeBase = knowledgeBaseEntry.getKey();
			for (Entry<String, Map<String, Long>> categoryEntry : knowledgeBaseEntry.getValue().entrySet()) {
				String category = categoryEntry.getKey();
				for (Entry<String, Long> targetEntry : categoryEntry.getValue().entrySet()) {
					String target = targetEntry.getKey();
					Long count = targetEntry.getValue();
					results.add(Arrays.asList(ResourceFactory.createStringLiteral(knowledgeBase.toString()),
							ResourceFactory.createStringLiteral(category), ResourceFactory.createStringLiteral(target),
							ResourceFactory.createTypedLiteral(count)));
				}
			}
		}
		preparedQuery.setRowValues("results", results);

		// construct result model
		Model resultModel = ModelUtils.getEmptyOntModel();
		Query query = preparedQuery.asQuery();
		QueryExecutionFactory.create(query, resultModel).execConstruct(resultModel);

		return resultModel;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {

	}
}
