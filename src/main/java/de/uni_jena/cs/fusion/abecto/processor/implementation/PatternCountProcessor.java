package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_NAME;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_TARGET;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.COUNT_MEASURE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.KNOWLEDGE_BASE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.VALUE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class PatternCountProcessor extends AbstractMetaProcessor<PatternCountProcessor.Parameter> {

	private static Query countQuery(String category, ElementGroup templatePattern, String target) {
		try {
			SelectBuilder select = new SelectBuilder();
			if (target != null) {
				select.addVar("count(distinct concat(str(?" + category + "),\" \",str(?" + target + ")))", "?count");
			} else {
				select.addVar("count(distinct ?" + category + ")", "?count");
			}
			Query query = select.build();
			query.setQueryPattern(templatePattern);
			return query;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Model computeResultModel() throws Exception {
		// get templates
		Collection<Category> categories = SparqlEntityManager.select(new Category(), this.metaModel);

		/**
		 * Counts by knowledge base, category and target. The null target key represents
		 * the count of entities.
		 */
		Map<UUID, Map<String, Map<String, Long>>> counts = new HashMap<>();

		for (Category category : categories) {
			// get template data
			String categoryName = category.name;
			ElementGroup categoryPattern = category.getPatternElementGroup();

			Collection<String> targets = new HashSet<>();
			category.getPatternVariables().forEach((v) -> targets.add(v.getName()));
			targets.remove(categoryName);
			targets.add(null);

			// create queries
			Map<String, Query> categoryQueries = new HashMap<>();
			targets.forEach((target) -> categoryQueries.put(target, countQuery(categoryName, categoryPattern, target)));

			for (Entry<UUID, Model> entry : this.inputGroupModels.entrySet()) {
				// get control variables
				UUID knowledgeBase = entry.getKey();
				Model inputGroupModel = entry.getValue();
				// get result map
				Map<String, Long> countsByTarget = counts.computeIfAbsent(knowledgeBase, (kb) -> new HashMap<>())
						.computeIfAbsent(categoryName, (c) -> new HashMap<>());

				for (String target : targets) {
					// get counts for current template, model and target
					ResultSet categoryQueryResult = QueryExecutionFactory
							.create(categoryQueries.get(target), inputGroupModel).execSelect();
					if (categoryQueryResult.hasNext()) {
						Long count = categoryQueryResult.next().getLiteral("count").getLong();
						// increase total count
						countsByTarget.compute(target, (k, v) -> (v == null) ? count : v + count);
					} else {
						throw new IllegalStateException();
					}
				}
			}
		}

		Node blankNodeVar = NodeFactory.createBlankNode();
		Node knowledgebaseVar = NodeFactory.createVariable("knowledgebase");
		Node categoryVar = NodeFactory.createVariable("category");
		Node targetVar = NodeFactory.createVariable("target");
		Node countVar = NodeFactory.createVariable("count");
		ConstructBuilder resultQueryBuilder = new ConstructBuilder().addConstruct(blankNodeVar, RDF.type, COUNT_MEASURE)
				.addConstruct(blankNodeVar, KNOWLEDGE_BASE, knowledgebaseVar)
				.addConstruct(blankNodeVar, CATEGORY_NAME, categoryVar)
				.addConstruct(blankNodeVar, CATEGORY_TARGET, targetVar).addConstruct(blankNodeVar, VALUE, countVar)
				.addValueVar(knowledgebaseVar).addValueVar(categoryVar).addValueVar(targetVar).addValueVar(countVar);

		// bind results
		for (Entry<UUID, Map<String, Map<String, Long>>> knowledgeBaseEntry : counts.entrySet()) {
			UUID knowledgeBase = knowledgeBaseEntry.getKey();
			for (Entry<String, Map<String, Long>> categoryEntry : knowledgeBaseEntry.getValue().entrySet()) {
				String category = categoryEntry.getKey();
				for (Entry<String, Long> targetEntry : categoryEntry.getValue().entrySet()) {
					String target = targetEntry.getKey();
					Long count = targetEntry.getValue();
					resultQueryBuilder.addValueRow(ResourceFactory.createStringLiteral(knowledgeBase.toString()),
							ResourceFactory.createStringLiteral(category),
							(target != null) ? ResourceFactory.createStringLiteral(target) : null,
							ResourceFactory.createTypedLiteral(count));
				}
			}
		}

		// construct result model
		Model resultModel = Models.getEmptyOntModel();
		Query resultQuery = resultQueryBuilder.build();
		QueryExecutionFactory.create(resultQuery, resultModel).execConstruct(resultModel);

		return resultModel;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {

	}
}
