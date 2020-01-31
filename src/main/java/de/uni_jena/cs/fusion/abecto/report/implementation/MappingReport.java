package de.uni_jena.cs.fusion.abecto.report.implementation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.PositiveMapping;
import de.uni_jena.cs.fusion.abecto.report.Report;
import de.uni_jena.cs.fusion.abecto.report.model.MappingReportEntity;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class MappingReport implements Report {

	@Override
	public Object of(Processor<?> processor)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		// get meta model union
		OntModel metaModel = Models.union(processor.getMetaModels());

		// get categories
		Collection<Category> categories = SparqlEntityManager.select(new Category(), metaModel);

		// get knowledge bases
		Collection<UUID> knowledgeBases = processor.getDataModels().keySet();

		// get knowledge base model unions
		Map<UUID, Model> dataModels = new HashMap<>();
		for (UUID knowlegeBase : knowledgeBases) {
			dataModels.put(knowlegeBase, Models.union(processor.getDataModels().get(knowlegeBase)));
		}

		for (Category category : categories) {

		// TODO get list of entities per category and knowledge base
		// TODO create MappingReportEntity for uncovered entities without other entity
//			category.selectCategory(model)
//			Map<UUID,Collection<>>
		}


		return SparqlEntityManager.select(PositiveMapping.prototype, metaModel).stream().map(MappingReportEntity::new)
				.collect(Collectors.toList());
	}
	
//	private static Collection<String> getEntities

}
