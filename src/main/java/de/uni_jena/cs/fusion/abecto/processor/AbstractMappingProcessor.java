package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.NegativeMapping;
import de.uni_jena.cs.fusion.abecto.processor.model.PositiveMapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public abstract class AbstractMappingProcessor<P extends ParameterModel> extends AbstractMetaProcessor<P>
		implements MappingProcessor<P> {

	@Override
	public Model computeResultModel() throws Exception {
		// collect known mappings
		Collection<Mapping> knownMappings = SparqlEntityManager.select(PositiveMapping.prototype, this.metaModel);
		knownMappings.addAll(SparqlEntityManager.select(NegativeMapping.prototype, this.metaModel));

		// init result model
		Model resultsModel = ModelFactory.createDefaultModel();

		for (Entry<UUID, Model> i : this.inputGroupModels.entrySet()) {
			for (Entry<UUID, Model> j : this.inputGroupModels.entrySet()) {
				if (i.getKey().compareTo(j.getKey()) > 0) {
					// compute mapping
					Collection<Mapping> mappings = computeMapping(i.getValue(), j.getValue());

					Collection<PositiveMapping> positiveMappings = new HashSet<>();
					Collection<NegativeMapping> negativeMappings = new HashSet<>();

					for (Mapping mapping : mappings) {
						// check if mapping is already known or contradicts to previous known mappings
						if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {

							if (mapping instanceof PositiveMapping) {
								positiveMappings.add((PositiveMapping) mapping);
							} else if (mapping instanceof NegativeMapping) {
								negativeMappings.add((NegativeMapping) mapping);
							} else {
								throw new IllegalStateException(String.format("Mapping is neither a %s nor a %s.",
										PositiveMapping.class.getName(), NegativeMapping.class.getName()));
							}
						}
					}
					// add mappings to results
					SparqlEntityManager.insert(positiveMappings, resultsModel);
					SparqlEntityManager.insert(negativeMappings, resultsModel);
				}
			}
		}

		return resultsModel;
	}

	/**
	 * Compute the mapping of two models.
	 * 
	 * @param firstModel  first model to process
	 * @param secondModel second model to process
	 * @return computed mapping
	 */
	public abstract Collection<Mapping> computeMapping(Model firstModel, Model secondModel) throws Exception;

}
