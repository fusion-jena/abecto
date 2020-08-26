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
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.util.Mappings;

public abstract class AbstractMappingProcessor<P extends ParameterModel> extends AbstractMetaProcessor<P>
		implements MappingProcessor<P> {

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
	public abstract Collection<Mapping> computeMapping(Model model1, Model model2, UUID ontologyId1, UUID ontologyId2)
			throws Exception;

	@Override
	public final void computeResultModel() throws Exception {
		// collect known mappings
		Collection<Mapping> knownMappings = Mappings.getMappings(this.metaModel);

		for (Entry<UUID, Model> i : this.inputGroupModels.entrySet()) {
			UUID ontologyId1 = i.getKey();
			for (Entry<UUID, Model> j : this.inputGroupModels.entrySet()) {
				UUID ontologyId2 = j.getKey();
				if (ontologyId1.compareTo(ontologyId2) > 0) {
					Collection<Mapping> newMappings = computeMapping(i.getValue(), j.getValue(), ontologyId1,
							ontologyId2);
					Collection<Mapping> acceptedMappings = Mappings.filterMappings(newMappings, knownMappings);
					Mappings.saveMappings(acceptedMappings, this.getResultModel());
				}
			}
		}
	}

}
