package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_NAME;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_TEMPLATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.ModelUtils;
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
		ParameterizedSparqlString preparedQuery = new ParameterizedSparqlString();
		preparedQuery.setCommandText("CONSTRUCT {[ <" + RDF.type + "> <" + CATEGORY + "> ; <" + CATEGORY_NAME
				+ "> ?name; <" + CATEGORY_TEMPLATE + "> ?pattern; ]} WHERE {VALUES (?name ?pattern) {?values} }");

		if (this.getParameters().patterns.isEmpty()) {
			throw new IllegalArgumentException("Empty category list.");
		}

		List<List<? extends RDFNode>> values = new ArrayList<>();
		for (Entry<String, Collection<String>> entry : this.getParameters().patterns.entrySet()) {
			String category = entry.getKey();
			if (entry.getValue().isEmpty()) {
				throw new IllegalArgumentException("Empty pattern list.");
			}
			for (String pattern : entry.getValue()) {
				Pattern.validate(category, pattern);
				values.add(Arrays.asList(ResourceFactory.createStringLiteral(category),
						ResourceFactory.createStringLiteral(pattern)));
			}
		}

		preparedQuery.setRowValues("values", values);
		Query query = preparedQuery.asQuery();
		Model model = ModelUtils.getEmptyOntModel();
		return QueryExecutionFactory.create(query, model).execConstruct(model);
	}

}
