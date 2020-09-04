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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

		Set<Mapping> newMappings = new HashSet<>();

		Map<Resource, Set<Resource>> negativeMappingSets = new HashMap<>();
		Map<Resource, Set<Resource>> positiveMappingSets = new HashMap<>();

		// process negative mappings
		Set<Mapping> knownNegativeMappings = Mappings.getNegativeMappings(this.metaModel);
		for (Mapping mapping : knownNegativeMappings) {
			Resource resource1 = mapping.resource1;
			Resource resource2 = mapping.resource2;

			negativeMappingSets.computeIfAbsent(resource1, (r) -> new HashSet<>()).add(resource2);
			negativeMappingSets.computeIfAbsent(resource2, (r) -> new HashSet<>()).add(resource1);
		}

		// process positive mappings
		Set<Mapping> knownPositiveMappings = Mappings.getPositiveMappings(this.metaModel);
		for (Mapping mapping : knownPositiveMappings) {
			if (!negativeMappingSets.getOrDefault(mapping.resource1, Collections.emptySet())
					.contains(mapping.resource2)) {
				// merge negative mappings
				Set<Resource> negativeMappingSet = negativeMappingSets.computeIfAbsent(mapping.resource1,
						(r) -> new HashSet<>());
				negativeMappingSet
						.addAll(negativeMappingSets.computeIfAbsent(mapping.resource2, (r) -> Collections.emptySet()));
				negativeMappingSets.put(mapping.resource2, negativeMappingSet);
				// add to and merge mappings
				Set<Resource> positiveMappingSet = positiveMappingSets.computeIfAbsent(mapping.resource1,
						(r) -> new HashSet<>(Collections.singleton(mapping.resource1)));
				positiveMappingSet.addAll(
						positiveMappingSets.getOrDefault(mapping.resource2, Collections.singleton(mapping.resource2)));
				positiveMappingSets.put(mapping.resource2, positiveMappingSet);
			}
		}

		// iterate negative mapping sets
		for (Resource resource1 : negativeMappingSets.keySet()) {
			for (Resource resource2 : negativeMappingSets.get(resource1)) {
				Mapping newMapping = Mapping.not(resource1, resource2);
				if (!knownNegativeMappings.contains(newMapping)) {
					newMappings.add(newMapping);
				}
			}
		}

		// iterate positive mapping sets (only once by creating a set of sets first)
		for (Set<Resource> positiveMappingSet : new HashSet<>(positiveMappingSets.values())) {
			for (Resource resource1 : positiveMappingSet) {
				for (Resource resource2 : positiveMappingSet) {
					if (!resource1.equals(resource2)) {
						Mapping newMapping = Mapping.of(resource1, resource2);
						if (!knownPositiveMappings.contains(newMapping)) {
							newMappings.add(newMapping);
						}
					}
				}
			}
		}

		Mappings.saveMappings(newMappings, this.getResultModel());
	}
}
