package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class ManualCategoryProcessor extends AbstractMetaProcessor<ManualCategoryProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Map<String, Collection<String>> patterns;
	}

	@Override
	protected Model computeResultModel() throws Exception {
		Collection<Category> categories = new ArrayList<>();

		if (this.getParameters().patterns.isEmpty()) {
			throw new IllegalArgumentException("Empty category list.");
		}

		for (Entry<String, Collection<String>> entry : this.getParameters().patterns.entrySet()) {
			String categoryName = entry.getKey();
			if (entry.getValue().isEmpty()) {
				throw new IllegalArgumentException("Empty pattern list.");
			}
			for (String categoryPattern : entry.getValue()) {
				categories.add(new Category(categoryName, categoryPattern));
			}
		}

		Model resultModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(categories, resultModel);
		return resultModel;
	}

}
