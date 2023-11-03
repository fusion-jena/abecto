/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.similarity.jarowinkler.JaroWinklerSimilarity;

public class JaroWinklerMappingProcessor extends MappingProcessor<JaroWinklerMappingProcessor> {

	// TODO add language handling parameter

	@Parameter
	public Resource aspect;
	@Parameter
	public Collection<String> variables;
	@Parameter
	public Double threshold;
	@Parameter
	public Boolean caseSensitive;

	@Override
	public void mapDatasets(Resource dataset1, Resource dataset2) {

		// get parameters
		Aspect aspect = this.getAspects().get(this.aspect);

		if (!aspect.coversDataset(dataset1) || !aspect.coversDataset(dataset2)) {
			return;
		}

		// make index case-insensitive, if requested
		Function<RDFNode, String> modifier;
		if (this.caseSensitive) {
			modifier = t -> t != null ? t.asLiteral().getString() : null;
		} else {
			modifier = t -> t != null ? t.asLiteral().getString().toLowerCase() : null;
		}

		// get resource indices
		Map<String, Map<String, Set<Resource>>> valuesByVariable1 = Aspect.getResourceIndex(aspect, dataset1,
				this.variables, this.getInputPrimaryModelUnion(dataset1), modifier);
		Map<String, Map<String, Set<Resource>>> valuesByVariable2 = Aspect.getResourceIndex(aspect, dataset2,
				this.variables, this.getInputPrimaryModelUnion(dataset2), modifier);

		// iterate variables
		for (String variable : valuesByVariable1.keySet()) {
			if (valuesByVariable1.containsKey(variable) && valuesByVariable2.containsKey(variable)) {

				Map<String, Set<Resource>> values1 = valuesByVariable1.get(variable);
				Map<String, Set<Resource>> values2 = valuesByVariable2.get(variable);

				JaroWinklerSimilarity<String> matcher1 = JaroWinklerSimilarity.with(values1.keySet(), this.threshold);
				JaroWinklerSimilarity<String> matcher2 = JaroWinklerSimilarity.with(values2.keySet(), this.threshold);

				// match from first to second
				Map<String, Collection<String>> matches1 = new HashMap<>();
				for (String value1 : values1.keySet()) {
					matches1.put(value1, maxValue(matcher2.apply(value1)));
				}

				// match from second to first
				for (String value2 : values2.keySet()) {
					for (String value1 : maxValue(matcher1.apply(value2))) {
						if (matches1.get(value1).contains(value2)) { // is bidirectional match
							/*
							 * NOTE: bidirectional matches are required to make the processor commutative
							 * regarding dataset order
							 */

							// convert match into mappings
							for (Resource resource1 : values1.get(value1)) {
								for (Resource resource2 : values2.get(value2)) {
									this.addCorrespondence(resource1, resource2);
								}
							}
						}
					}
				}
			}
		}
	}

	private Collection<String> maxValue(Map<String, Double> map) {
		List<String> bestMatches = new ArrayList<>();
		double maxSimilarity = 0d;
		for (Entry<String, Double> entry : map.entrySet()) {
			if (entry.getValue() < maxSimilarity) {
				// do nothing
			} else if (entry.getValue().equals(maxSimilarity)) {
				bestMatches.add(entry.getKey());
			} else {
				bestMatches.clear();
				maxSimilarity = entry.getValue();
				bestMatches.add(entry.getKey());
			}
		}
		return bestMatches;
	}
}