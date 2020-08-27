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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.metaentity.Measurement;
import de.uni_jena.cs.fusion.abecto.metaentity.Omission;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Mappings;

public class CompletenessProcessor extends AbstractMetaProcessor<CompletenessProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Optional<Collection<String>> omission = Optional.empty();
		public Optional<Collection<String>> coverage_absolute = Optional.empty();
		public Optional<Collection<String>> coverage_relative = Optional.empty();
		public Optional<Collection<String>> duplicate = Optional.empty();
		public Optional<Collection<String>> completeness = Optional.empty();
	}

	@Override
	protected void computeResultModel() throws Exception {
		Set<Mapping> mappings = Mappings.getPositiveMappings(this.metaModel);

		// get resources by ontology and category
		Map<String, Map<UUID, Collection<Resource>>> resourcesByOntologyByCategory = new HashMap<>();
		for (Category category : SparqlEntityManager.select(new Category(), this.metaModel)) {
			resourcesByOntologyByCategory.computeIfAbsent(category.name, (o) -> {
				return new HashMap<UUID, Collection<Resource>>();
			}).put(category.ontology, category.selectCategoryResources(this.inputGroupModels.get(category.ontology)));
		}

		// get mapped resources by resources
		Map<Resource, Set<Resource>> mappedResourcesByResource = new HashMap<>();
		for (Mapping mapping : mappings) {
			mappedResourcesByResource.computeIfAbsent(mapping.resource1, (r) -> new HashSet<Resource>())
					.add(mapping.resource2);
			mappedResourcesByResource.computeIfAbsent(mapping.resource2, (r) -> new HashSet<Resource>())
					.add(mapping.resource1);
		}

		Set<Omission> omissions = new HashSet<>();
		Set<Issue> issues = new HashSet<>();
		Set<Measurement> measurements = new HashSet<>();

		/** coverage by second ontology of first ontology per category */
		Map<String, Map<UUID, Map<UUID, Long>>> coverageOfOntologyByOntologyByCategory = new HashMap<>();

		for (String categoryName : resourcesByOntologyByCategory.keySet()) {
			Map<UUID, Collection<Resource>> resourcesByOntology = resourcesByOntologyByCategory.get(categoryName);
			Map<UUID, Map<UUID, Long>> coverageOfOntologyByOntology = coverageOfOntologyByOntologyByCategory
					.computeIfAbsent(categoryName, (c) -> new HashMap<>());
			for (UUID ontologyId1 : resourcesByOntology.keySet()) {
				Map<UUID, Long> coverageOfOntology = coverageOfOntologyByOntology.computeIfAbsent(ontologyId1,
						(o) -> new HashMap<>());
				for (UUID ontologyId2 : resourcesByOntology.keySet()) {
					Collection<Resource> resources1 = resourcesByOntology.get(ontologyId1);
					if (!ontologyId1.equals(ontologyId2)) {
						if (isEnabled(this.getParameters().coverage_absolute, categoryName)
								|| isEnabled(this.getParameters().coverage_relative, categoryName)
								|| isEnabled(this.getParameters().omission, categoryName)
								|| isEnabled(this.getParameters().completeness, categoryName)) {
							Long coverage = coverageOfOntology.computeIfAbsent(ontologyId2, (o) -> Long.valueOf(0l));
							Collection<Resource> resources2 = resourcesByOntology.get(ontologyId2);
							for (Resource resource1 : resources1) {
								// get mapped resources of ontology 2
								Set<Resource> mappedResources = new HashSet<>(
										mappedResourcesByResource.getOrDefault(resource1, Collections.emptySet()));
								mappedResources.retainAll(resources2);
								if (mappedResources.isEmpty()) {
									if (isEnabled(this.getParameters().omission, categoryName)) {
										// report omission
										omissions.add(
												new Omission(null, categoryName, ontologyId2, resource1, ontologyId1));
									}
								} else {
									// count covered resources
									coverage += 1;
								}
							}
							// absolute coverage
							if (isEnabled(this.getParameters().coverage_absolute, categoryName)) {
								measurements.add(new Measurement(null, ontologyId2, "Coverage (absolute)", coverage,
										Optional.of("of category"), Optional.of(categoryName),
										Optional.of("in ontology"), Optional.of(ontologyId1.toString())));
							}
							// relative coverage
							if (isEnabled(this.getParameters().coverage_relative, categoryName)) {
								measurements.add(new Measurement(null, ontologyId2, "Coverage (relative in %)",
										coverage * 100 / resources1.size(), Optional.of("of category"),
										Optional.of(categoryName), Optional.of("in ontology"),
										Optional.of(ontologyId1.toString())));
							}
						}
					} else if (isEnabled(this.getParameters().duplicate, categoryName)) {
						for (Resource resource1 : resources1) {
							if (mappedResourcesByResource.containsKey(resource1)) {
								// get mapped resources of same ontology
								Set<Resource> mappedResources = new HashSet<>(
										mappedResourcesByResource.getOrDefault(resource1, Collections.emptySet()));
								mappedResources.retainAll(resources1);
								// report duplicates
								if (!mappedResources.isEmpty()) {
									List<String> duplicates = new ArrayList<>();
									mappedResources.stream().map(Resource::getURI).forEach(duplicates::add);
									Collections.sort(duplicates);
									issues.add(new Issue(null, ontologyId1, resource1, "Duplicated Resource",
											"Duplicates: " + String.join(", ", duplicates)));
								}
							}
						}
					}
				}
			}
		}

		// completeness
//		for (String categoryName : coverageOfOntologyByOntologyByCategory.keySet()) {
//			if (isEnabled(this.getParameters().completeness, categoryName)) {
//				Map<UUID, Map<UUID, Long>> coverageOfOntologyByOntology = coverageOfOntologyByOntologyByCategory
//						.get(categoryName);
//				// TODO completeness calculation
//			}
//		}

		// write results
		SparqlEntityManager.insert(omissions, this.getResultModel());
		SparqlEntityManager.insert(measurements, this.getResultModel());
		SparqlEntityManager.insert(issues, this.getResultModel());
	}

	private boolean isEnabled(Optional<Collection<String>> parameter, String categoryName) {
		return parameter.isEmpty() || parameter.get().contains(categoryName);
	}
}
