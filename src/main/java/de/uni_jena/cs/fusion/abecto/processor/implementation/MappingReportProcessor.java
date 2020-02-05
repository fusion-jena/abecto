package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractReportProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.MappingReportEntity;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class MappingReportProcessor extends AbstractReportProcessor<EmptyParameters> {

	@Override
	protected Model computeResultModel() throws Exception {
		// get meta model union
		OntModel metaModel = this.metaModel;

		// get categories
		Collection<Category> categories = SparqlEntityManager.select(new Category(), metaModel);

		Collection<MappingReportEntity> mappingReportEntities = new ArrayList<>();
		Map<String, Collection<Category>> categoriesByName = new HashMap<>();

		for (Category category : categories) {
			categoriesByName.computeIfAbsent(category.name, (x) -> {
				return new HashSet<Category>();
			}).add(category);
		}

		for (String categoryName : categoriesByName.keySet()) {
			// generate entity collections per knowledge base and category
			Map<UUID, Map<String, SortedMap<String, RDFNode>>> entitiesByKnowledgeBaseAndKey = new HashMap<>();
			for (Category category : categoriesByName.get(categoryName)) {
				Map<String, SortedMap<String, RDFNode>> entitiesByKey = entitiesByKnowledgeBaseAndKey
						.computeIfAbsent(category.knowledgeBase, (x) -> {
							return new HashMap<>();
						});
				ResultSet entitiesQueryResult = category
						.selectCategory(this.inputGroupModels.get(category.knowledgeBase));
				entitiesQueryResult.forEachRemaining((querySolution) -> {
					SortedMap<String, RDFNode> values = new TreeMap<>();
					querySolution.varNames().forEachRemaining((varName) -> {
						values.put(varName, querySolution.get(varName));
					});
					entitiesByKey.put(querySolution.get(categoryName).asResource().getURI(), values);
				});
			}

			// generate report entities
			for (UUID knowledgeBase1 : entitiesByKnowledgeBaseAndKey.keySet()) {
				Map<String, SortedMap<String, RDFNode>> entitiesOfKnowledgeBase1 = entitiesByKnowledgeBaseAndKey
						.get(knowledgeBase1);
				for (UUID knowledgeBase2 : entitiesByKnowledgeBaseAndKey.keySet()) {
					if (knowledgeBase1.compareTo(knowledgeBase2) > 0) {
						Map<String, SortedMap<String, RDFNode>> entitiesOfKnowledgeBase2 = entitiesByKnowledgeBaseAndKey
								.get(knowledgeBase2);
						// initialize unmapped entity sets of current knowledge base pair
						Collection<String> unmappedEntities1 = entitiesOfKnowledgeBase1.keySet();
						Collection<String> unmappedEntities2 = entitiesOfKnowledgeBase2.keySet();
						// add mappings to results
						SparqlEntityManager
								.select(Arrays.asList(Mapping.of(knowledgeBase1, knowledgeBase2, categoryName),
										Mapping.of(knowledgeBase2, knowledgeBase1, categoryName)), this.metaModel)
								.forEach((mapping) -> {
									SortedMap<String, RDFNode> entityData1 = entitiesOfKnowledgeBase1
											.get(mapping.getResourceOf(knowledgeBase1).getURI());
									SortedMap<String, RDFNode> entityDate2 = entitiesOfKnowledgeBase2
											.get(mapping.getResourceOf(knowledgeBase2).getURI());
									mappingReportEntities
											.add(new MappingReportEntity(mapping, entityData1, entityDate2));
									unmappedEntities1.remove(mapping.getResourceOf(knowledgeBase1).getURI());
									unmappedEntities2.remove(mapping.getResourceOf(knowledgeBase2).getURI());
								});
						// add missing mappings to results
						for (String unmappedEntity : unmappedEntities1) {
							mappingReportEntities.add(new MappingReportEntity(unmappedEntity, (String) null,
									entitiesOfKnowledgeBase1.get(unmappedEntity), (SortedMap<String, RDFNode>) null,
									knowledgeBase1.toString(), knowledgeBase2.toString(), categoryName));
						}
						for (String unmappedEntity : unmappedEntities2) {
							mappingReportEntities.add(new MappingReportEntity((String) null, unmappedEntity,
									(SortedMap<String, RDFNode>) null, entitiesOfKnowledgeBase2.get(unmappedEntity),
									knowledgeBase1.toString(), knowledgeBase2.toString(), categoryName));
						}
					}
				}
			}
		}

		OntModel resultModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(mappingReportEntities, resultModel);
		return resultModel;
	}

}
