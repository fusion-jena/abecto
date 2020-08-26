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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.util.Mappings;

public class TransitiveMappingProcessor extends AbstractMetaProcessor<EmptyParameters>
		implements MappingProcessor<EmptyParameters> {

	@Override
	public final void computeResultModel() throws Exception {

		Set<Mapping> knownMappings = Mappings.getMappings(this.metaModel);
		LinkedList<Mapping> unprocessedMappings = new LinkedList<>(knownMappings);
		Set<Mapping> newMappings = new HashSet<>();

		while (!unprocessedMappings.isEmpty()) {
			Mapping mapping1 = unprocessedMappings.poll();

			// check if mapping1 was overruled by a suppressed mapping
			if (!knownMappings.contains(mapping1) && !newMappings.contains(mapping1)) {
				// do not process mapping1 as it was overruled by a suppressed mapping
				break;
			}

			ListIterator<Mapping> unprocessedMappingsIterator = unprocessedMappings.listIterator();
			while (unprocessedMappingsIterator.hasNext()) {
				Mapping mapping2 = unprocessedMappingsIterator.next();

				// check if mapping2 was overruled by a suppressed mapping
				if (!knownMappings.contains(mapping2) && !newMappings.contains(mapping2)) {
					// do not further process mapping2 as it was overruled by a suppressed mapping
					unprocessedMappingsIterator.remove();
					break;
				}

				// create new mapping, if applicable
				Resource resource1, resource2;
				if (mapping1.resource1.equals(mapping2.resource1)) {
					resource1 = mapping1.resource2;
					resource2 = mapping2.resource2;
				} else if (mapping1.resource1.equals(mapping2.resource2)) {
					resource1 = mapping1.resource2;
					resource2 = mapping2.resource1;
				} else if (mapping1.resource2.equals(mapping2.resource1)) {
					resource1 = mapping1.resource1;
					resource2 = mapping2.resource2;
				} else if (mapping1.resource2.equals(mapping2.resource2)) {
					resource1 = mapping1.resource1;
					resource2 = mapping2.resource1;
				} else {
					break;
				}
				Mapping newMapping;
				if (mapping1.resourcesMap && mapping2.resourcesMap) {
					newMapping = Mapping.of(resource1, resource2);
				} else if (mapping1.resourcesMap != mapping2.resourcesMap) {
					newMapping = Mapping.not(resource1, resource2);
				} else {
					break;
				}

				// cache inverse mapping
				Mapping inverseNewMapping = newMapping.inverse();

				// add mapping if it is new and does not contradict to known mappings
				if (!knownMappings.contains(newMapping) && !knownMappings.contains(inverseNewMapping)) {
					if (newMapping.resourcesMap) {
						// add mapping if it does not contradict to a new suppressing mapping
						if (!newMappings.contains(inverseNewMapping) && newMappings.add(newMapping)) {
							unprocessedMappingsIterator.add(newMapping);
						}
					} else {
						// add new suppressing mapping and remove contradicting new mapping
						if (newMappings.add(newMapping)) {
							unprocessedMappingsIterator.add(newMapping);
							newMappings.remove(inverseNewMapping);
							// NOTE: inverseNewMapping remains in unprocessedMappings /
							// unprocessedMappingsIterator but will not be processed
						}
					}
				}
			}
		}
		Mappings.saveMappings(newMappings, this.getResultModel());
	}
}
