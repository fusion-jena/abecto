package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.pattern.Pattern;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.util.Queries;

public class ManualPatternProcessor extends AbstractMetaProcessor<ManualPatternProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Map<String, Collection<String>> patterns;
	}

	@Override
	protected Model computeResultModel() throws Exception {
		ConstructBuilder resultQuery = Queries.patternConstruct();

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
				resultQuery.addValueRow(ResourceFactory.createStringLiteral(category),
						ResourceFactory.createStringLiteral(pattern));
			}
		}

		Model resultModel = Models.getEmptyOntModel();
		return QueryExecutionFactory.create(resultQuery.build(), resultModel).execConstruct(resultModel);
	}

}
