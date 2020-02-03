package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractReportProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.MappingReportEntity;
import de.uni_jena.cs.fusion.abecto.processor.model.PositiveMapping;
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
			Map<UUID, Map<String, Map<String, RDFNode>>> entitiesByKnowledgeBaseAndKey = new HashMap<>();
			for (Category category : categoriesByName.get(categoryName)) {
				Map<String, Map<String, RDFNode>> entitiesByKey = entitiesByKnowledgeBaseAndKey
						.put(category.knowledgeBase, new HashMap<>());
				ResultSet entitiesQueryResult = category
						.selectCategory(this.inputGroupModels.get(category.knowledgeBase));
				entitiesQueryResult.forEachRemaining((querySolution) -> {
					Map<String, RDFNode> values = new HashMap<>();
					querySolution.varNames().forEachRemaining((varName) -> {
						values.put(varName, querySolution.get(varName));
					});
					entitiesByKey.put(querySolution.get(categoryName).asResource().getURI(), values);
				});
			}

			// generate report entities
			for (UUID knowledgeBase1 : entitiesByKnowledgeBaseAndKey.keySet()) {
				Map<String, Map<String, RDFNode>> entitiesOfKnowledgeBase1 = entitiesByKnowledgeBaseAndKey
						.get(knowledgeBase1);
				for (UUID knowledgeBase2 : entitiesByKnowledgeBaseAndKey.keySet()) {
					Map<String, Map<String, RDFNode>> entitiesOfKnowledgeBase2 = entitiesByKnowledgeBaseAndKey
							.get(knowledgeBase2);
					// initialize unmapped entity sets of current knowledge base pair
					Collection<String> unmappedEntities1 = entitiesOfKnowledgeBase1.keySet();
					Collection<String> unmappedEntities2 = entitiesOfKnowledgeBase2.keySet();
					// add mappings to results
					if (knowledgeBase1.compareTo(knowledgeBase2) > 0) {
						SparqlEntityManager
								.select(new PositiveMapping((Resource) null, (Resource) null,
										Optional.of(knowledgeBase1), Optional.of(knowledgeBase1),
										Optional.of(categoryName), Optional.of(categoryName)), this.metaModel)
								.forEach((mapping) -> {
									Map<String, RDFNode> entityData1 = entitiesOfKnowledgeBase1
											.get(mapping.first.getURI());
									Map<String, RDFNode> entityDate2 = entitiesOfKnowledgeBase2
											.get(mapping.second.getURI());
									mappingReportEntities
											.add(new MappingReportEntity(mapping, entityData1, entityDate2));
									unmappedEntities1.remove(mapping.first.getURI());
									unmappedEntities2.remove(mapping.second.getURI());
								});
					}
					// add missing mappings to results
					for (String unmappedEntity : unmappedEntities1) {
						mappingReportEntities.add(new MappingReportEntity(unmappedEntity, (String) null,
								entitiesOfKnowledgeBase1.get(unmappedEntity), (Map<String, RDFNode>) null,
								knowledgeBase1.toString(), knowledgeBase2.toString(), categoryName));
					}
					for (String unmappedEntity : unmappedEntities2) {
						mappingReportEntities.add(new MappingReportEntity((String) null, unmappedEntity,
								(Map<String, RDFNode>) null, entitiesOfKnowledgeBase1.get(unmappedEntity),
								knowledgeBase1.toString(), knowledgeBase2.toString(), categoryName));
					}
				}
			}
		}

		OntModel resultModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(mappingReportEntities, resultModel);
		return resultModel;
	}

}
