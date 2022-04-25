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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Parameter;

/**
 * Provides correspondences based on equivalent values of resources in different
 * datasets. Corresponding Resources are treated as equivalent values. If there
 * exists multiple values for one variable, only one pair of values must be
 * equivalent.
 */
public class EquivalentValueMappingProcessor extends MappingProcessor<EquivalentValueMappingProcessor> {

	final static Logger log = LoggerFactory.getLogger(EquivalentValueMappingProcessor.class);

	@Parameter
	public Resource aspect;
	@Parameter
	public List<String> variables;

	@Override
	public void mapDatasets(Resource dataset1, Resource dataset2) {
		Aspect aspect = this.getAspects().get(this.aspect);

		// check if aspect patterns contain all variables
		for (Resource dataset : new Resource[] { dataset1, dataset2 }) {
			try {
				Collection<String> patternVariables = aspect.getPattern(dataset).getResultVars();
				if (!patternVariables.containsAll(variables)) {
					ArrayList<String> missingVariables = new ArrayList<String>(variables);
					missingVariables.removeAll(patternVariables);
					log.warn("Missing variable(s) in pattern of aspect {} and dataset {}: {}", aspect, dataset,
							missingVariables);
					return;
				}
			} catch (NullPointerException e) {
				log.warn("No pattern for aspect {} and dataset {} defined.", aspect, dataset);
				return;
			}
		}

		Map<String, Map<RDFNode, Set<Resource>>> resourceIndex2 = Aspect.getResourceIndex(aspect, dataset2, variables,
				this.getInputPrimaryModelUnion(dataset2));
		for (Resource resource1 : Aspect.getResourceKeys(aspect, dataset1, this.getInputPrimaryModelUnion(dataset1))) {
			Map<String, Set<RDFNode>> resourceValues1 = Aspect
					.getResource(aspect, dataset1, resource1, this.getInputPrimaryModelUnion(dataset1)).orElseThrow();
			HashSet<Resource> correspondingResources = null;
			for (String variable : variables) {
				HashSet<Resource> variableCandidates = new HashSet<>();
				for (RDFNode value : resourceValues1.get(variable)) {
					if (value.isResource()) {
						for (Resource valueResource : getCorrespondenceGroup(value.asResource())) {
							variableCandidates.addAll(
									resourceIndex2.get(variable).getOrDefault(valueResource, Collections.emptySet()));
						}
					} else {
						variableCandidates
								.addAll(resourceIndex2.get(variable).getOrDefault(value, Collections.emptySet()));
					}
				}
				if (correspondingResources == null) {
					correspondingResources = variableCandidates;
				} else {
					correspondingResources.retainAll(variableCandidates);
				}
				if (correspondingResources.isEmpty()) {
					break;
				}
			}
			correspondingResources.add(resource1);
			addCorrespondence(this.aspect, correspondingResources);
		}

	}
}
