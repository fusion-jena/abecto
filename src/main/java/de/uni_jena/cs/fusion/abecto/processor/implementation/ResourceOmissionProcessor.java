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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.metaentity.Omission;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Metadata;

public class ResourceOmissionProcessor extends Processor {

	/** Categories to process. */
	@Parameter
	public Collection<String> categories;

	@Override
	public void run() {
		Model resultModel = this.getResultModel();

		Collection<String> categoryNames = this.categories;

		for (String categoryName : categoryNames) {
			Set<Category> categories = SparqlEntityManager.select(new Category(categoryName, null, null),
					this.metaModel);
			Map<UUID, Collection<Resource>> resourcesByOntology = new HashMap<>();

			for (Category category : categories) {
				Model ontologyModel = this.inputGroupModels.get(category.ontology);
				Collection<Resource> ontologyResources = category.selectCategoryResources(ontologyModel);
				resourcesByOntology.put(category.ontology, ontologyResources);
			}

			// load mapping
			Map<Resource, Set<Resource>> mappings = new HashMap<>();
			for (Mapping mapping : Metadata.getPositiveMappings(this.metaModel)) {
				mappings.computeIfAbsent(mapping.resource1, (x) -> {
					return new HashSet<>();
				}).add(mapping.resource2);
				mappings.computeIfAbsent(mapping.resource2, (x) -> {
					return new HashSet<>();
				}).add(mapping.resource1);
			}

			// identify omissions
			Collection<Omission> omissions = new ArrayList<>();
			for (UUID sourceOntology : resourcesByOntology.keySet()) {
				for (Resource missingResource : resourcesByOntology.get(sourceOntology)) {
					if (!mappings.containsKey(missingResource)) {
						for (UUID ontology : resourcesByOntology.keySet()) {
							if (!ontology.equals(sourceOntology)) {
								omissions.add(
										new Omission(null, categoryName, ontology, missingResource, sourceOntology));
							}
						}
					} else {
						for (UUID ontology : resourcesByOntology.keySet()) {
							if (mappings.get(missingResource).stream()
									.filter(resourcesByOntology.get(ontology)::contains).findAny().isEmpty()) {
								omissions.add(
										new Omission(null, categoryName, ontology, missingResource, sourceOntology));
							}
						}
					}
				}
			}
		}

		this.setModel(resultModel);
	}

}
