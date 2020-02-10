package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public abstract class AbstractMappingProcessor<P extends ParameterModel> extends AbstractMetaProcessor<P>
		implements MappingProcessor<P> {

	@Override
	public final Model computeResultModel() throws Exception {
		// collect known mappings
		Collection<Mapping> knownMappings = SparqlEntityManager.select(Mapping.of(), this.metaModel);
		knownMappings.addAll(SparqlEntityManager.select(Mapping.not(), this.metaModel));

		// init result model
		Model resultsModel = ModelFactory.createDefaultModel();

		for (Entry<UUID, Model> i : this.inputGroupModels.entrySet()) {
			UUID knowledgeBaseId1 = i.getKey();
			for (Entry<UUID, Model> j : this.inputGroupModels.entrySet()) {
				UUID knowledgeBaseId2 = j.getKey();
				if (knowledgeBaseId1.compareTo(knowledgeBaseId2) > 0) {
					// compute mapping
					Collection<Mapping> mappings = computeMapping(i.getValue(), j.getValue(), knowledgeBaseId1,
							knowledgeBaseId2);

					Collection<Mapping> acceptedMappings = new HashSet<>();

					for (Mapping mapping : mappings) {
						// check if mapping is already known or contradicts to previous known mappings
						if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {
							acceptedMappings.add(mapping);
						}
					}
					// add mappings to results
					SparqlEntityManager.insert(acceptedMappings, resultsModel);
				}
			}
		}

		return resultsModel;
	}

	/**
	 * Computes the mappings of two models. The mappings may contain category and
	 * knowledge base meta data.
	 * 
	 * @param model1           the first model to process
	 * @param model2           the second model to process
	 * @param knowledgeBaseId2 the knowledge base id of the first model
	 * @param knowledgeBaseId1 the knowledge base id of the second model
	 * @return the computed mappings
	 */
	public abstract Collection<Mapping> computeMapping(Model model1, Model model2, UUID knowledgeBaseId1,
			UUID knowledgeBaseId2) throws Exception;

}
