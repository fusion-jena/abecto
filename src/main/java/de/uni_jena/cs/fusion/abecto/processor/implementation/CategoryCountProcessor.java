package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.CategoryCountMeasure;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class CategoryCountProcessor extends AbstractMetaProcessor<CategoryCountProcessor.Parameter> {

	private static Query countQuery(String category, ElementGroup pattern, String target) {
		try {
			SelectBuilder select = new SelectBuilder();
			if (target != null) {
				select.addVar("count(distinct concat(str(?" + category + "),\" \",str(?" + target + ")))", "?count");
			} else {
				select.addVar("count(distinct ?" + category + ")", "?count");
			}
			Query query = select.build();
			query.setQueryPattern(pattern);
			return query;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Model computeResultModel() throws Exception {
		// get categories
		Collection<Category> categories = SparqlEntityManager.select(new Category(), this.metaModel);

		// initialize result set
		Collection<CategoryCountMeasure> categoryCountMeasuers = new ArrayList<CategoryCountMeasure>();

		for (Category category : categories) {
			// get category data
			ElementGroup categoryPattern = category.getPatternElementGroup();

			// get targets
			Collection<String> targets = new HashSet<>();
			category.getPatternVariables().forEach((v) -> targets.add(v.getName()));
			targets.remove(category.name);
			targets.add(null);

			// create queries
			Map<String, Query> categoryQueries = new HashMap<>();
			targets.forEach(
					(target) -> categoryQueries.put(target, countQuery(category.name, categoryPattern, target)));

			// use the model destined by the knowledge base parameter of the category
			Model knowledgeBaseModel = this.inputGroupModels.get(category.knowledgeBase);

			for (String target : targets) {
				// get counts for current category, knowledge base and target
				ResultSet categoryQueryResult = QueryExecutionFactory
						.create(categoryQueries.get(target), knowledgeBaseModel).execSelect();
				if (categoryQueryResult.hasNext()) {
					Long count = categoryQueryResult.next().getLiteral("count").getLong();
					// add count to results
					// TODO build sum for multiple entries per kb and name
					categoryCountMeasuers.add(new CategoryCountMeasure(category.name, Optional.ofNullable(target),
							count, category.knowledgeBase));
				} else {
					throw new IllegalStateException();
				}
			}
		}

		// construct result model
		Model resultModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(categoryCountMeasuers, resultModel);

		return resultModel;
	}

	@JsonSerialize
	public static class Parameter implements ParameterModel {

	}
}
