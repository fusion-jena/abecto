package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
						Set<String> unmappedEntities1 = new HashSet<>(entitiesOfKnowledgeBase1.keySet());
						Set<String> unmappedEntities2 = new HashSet<>(entitiesOfKnowledgeBase2.keySet());
						// add mappings to results
						Collection<Mapping> mappings = SparqlEntityManager
								.select(Arrays.asList(Mapping.of(knowledgeBase1, knowledgeBase2, categoryName),
										Mapping.of(knowledgeBase2, knowledgeBase1, categoryName)), this.metaModel);
						for (Mapping mapping : mappings) {
							String entity1 = mapping.getResourceOf(knowledgeBase1).getURI();
							String entity2 = mapping.getResourceOf(knowledgeBase2).getURI();
							SortedMap<String, RDFNode> entityData1 = entitiesOfKnowledgeBase1.get(entity1);
							SortedMap<String, RDFNode> entityDate2 = entitiesOfKnowledgeBase2.get(entity2);
							mappingReportEntities.add(new MappingReportEntity(entity1, entity2, entityData1,
									entityDate2, knowledgeBase1.toString(), knowledgeBase2.toString(), categoryName));
							unmappedEntities1.remove(entity1);
							unmappedEntities2.remove(entity2);
						}
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
