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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Measurement;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class CategoryCountProcessor extends AbstractMetaProcessor<EmptyParameters> {

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
	protected void computeResultModel() throws Exception {
		// get categories
		Collection<Category> categories = SparqlEntityManager.select(new Category(), this.metaModel);

		// initialize result set
		Collection<Measurement> measurements = new ArrayList<Measurement>();

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

			// use the model destined by the ontology parameter of the category
			Model ontologyModel = this.inputGroupModels.get(category.ontology);

			for (String target : targets) {
				// get counts for current category, ontology and target
				ResultSet categoryQueryResult = QueryExecutionFactory
						.create(categoryQueries.get(target), ontologyModel).execSelect();
				if (categoryQueryResult.hasNext()) {
					Long count = categoryQueryResult.next().getLiteral("count").getLong();
					// add count to results
					// TODO build sum for multiple entries per kb and name
					measurements.add(measurement(category.ontology, category.name, target, count));
				} else {
					throw new IllegalStateException();
				}
			}
		}

		// write into result model
		SparqlEntityManager.insert(measurements, this.getResultModel());
	}

	protected static Measurement measurement(UUID knowledgeBase, String categoryName, String variableName, Long value) {
		return new Measurement((Resource) null, knowledgeBase, "Category Count", value, Optional.of("Category"),
				Optional.of(categoryName), Optional.of("Variable"), Optional.ofNullable(variableName));
	}
}
