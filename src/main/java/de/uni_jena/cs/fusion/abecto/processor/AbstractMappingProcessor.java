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
package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public abstract class AbstractMappingProcessor<P extends ParameterModel> extends AbstractMetaProcessor<P>
		implements MappingProcessor<P> {

	@Override
	public final void computeResultModel() throws Exception {
		// collect known mappings
		Collection<Mapping> knownMappings = SparqlEntityManager.select(Mapping.any(), this.metaModel);

		for (Entry<UUID, Model> i : this.inputGroupModels.entrySet()) {
			UUID ontologyId1 = i.getKey();
			for (Entry<UUID, Model> j : this.inputGroupModels.entrySet()) {
				UUID ontologyId2 = j.getKey();
				if (ontologyId1.compareTo(ontologyId2) > 0) {
					// compute mapping
					Collection<Mapping> mappings = computeMapping(i.getValue(), j.getValue(), ontologyId1,
							ontologyId2);

					Collection<Mapping> acceptedMappings = new HashSet<>();

					for (Mapping mapping : mappings) {
						// check if mapping is already known or contradicts to previous known mappings
						if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {
							acceptedMappings.add(mapping);
						}
					}
					// add mappings to results
					SparqlEntityManager.insert(acceptedMappings, this.getResultModel());
				}
			}
		}
	}

	/**
	 * Computes the mappings of two models. The mappings may contain category meta
	 * data.
	 * 
	 * @param model1      the first model to process
	 * @param model2      the second model to process
	 * @param ontologyId1 the ontology id of the first model
	 * @param ontologyId2 the ontology id of the second model
	 * @return the computed mappings
	 */
	public abstract Collection<Mapping> computeMapping(Model model1, Model model2, UUID ontologyId1,
			UUID ontologyId2) throws Exception;

}
