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
package de.uni_jena.cs.fusion.abecto.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class Mappings {

	public static Set<Mapping> filterMappings(Collection<Mapping> newMappings, Collection<Mapping> knownMappings) {
		Set<Mapping> acceptedMappings = new HashSet<>();

		for (Mapping mapping : newMappings) {
			// check if mapping is already known or contradicts to previous known mappings
			if (!knownMappings.contains(mapping) && !knownMappings.contains(mapping.inverse())) {
				acceptedMappings.add(mapping);
			}
		}

		return acceptedMappings;
	}

	public static Set<Mapping> getMappings(Model metaModel)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		return SparqlEntityManager.select(Mapping.any(), metaModel);
	}

	public static Set<Mapping> getPositiveMappings(Model metaModel)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		return SparqlEntityManager.select(Mapping.of(), metaModel);
	}

	public static Set<Mapping> getNegativeMappings(Model metaModel)
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		return SparqlEntityManager.select(Mapping.not(), metaModel);
	}

	public static void saveMappings(Collection<Mapping> acceptedMappings, Model resultModel) {
		SparqlEntityManager.insert(acceptedMappings, resultModel);
	}
}
