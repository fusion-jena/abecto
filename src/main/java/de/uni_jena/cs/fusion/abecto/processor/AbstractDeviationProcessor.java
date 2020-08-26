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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.metaentity.Category;
import de.uni_jena.cs.fusion.abecto.metaentity.Deviation;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Mappings;

public abstract class AbstractDeviationProcessor<Parameter>
		extends AbstractMetaProcessor<AbstractDeviationProcessor.Parameter> {

	public static class Parameter implements ParameterModel {
		/**
		 * Variables to process by categories.
		 */
		public Map<String, Collection<String>> variables;
	}

	@Override
	public final void computeResultModel() throws Exception {
		Set<UUID> ontologyIds = this.inputGroupModels.keySet();

		Collection<Deviation> deviations = new ArrayList<>();

		// load mapping
		Map<Resource, Set<Resource>> mappings = new HashMap<>();
		for (Mapping mapping : Mappings.getPositiveMappings(this.metaModel)) {
			mappings.computeIfAbsent(mapping.resource1, (x) -> {
				return new HashSet<>();
			}).add(mapping.resource2);
			mappings.computeIfAbsent(mapping.resource2, (x) -> {
				return new HashSet<>();
			}).add(mapping.resource1);
		}

		// iterate ontology pairs
		for (UUID ontologyId1 : ontologyIds) {
			Model model1 = this.inputGroupModels.get(ontologyId1);
			for (UUID ontologyId2 : ontologyIds) {
				if (ontologyId1.compareTo(ontologyId2) > 0) { // do not do work twice
					Model model2 = this.inputGroupModels.get(ontologyId2);

					// iterate categories
					for (String categoryName : this.getParameters().variables.keySet()) {
						Optional<Category> category1Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, ontologyId1), this.metaModel);
						Optional<Category> category2Optional = SparqlEntityManager
								.selectOne(new Category(categoryName, null, ontologyId2), this.metaModel);
						if (category1Optional.isPresent() && category2Optional.isPresent()) {
							Category category1 = category1Optional.orElseThrow();
							Category category2 = category2Optional.orElseThrow();

							Collection<String> variableNames = this.getParameters().variables.get(categoryName);

							deviations.addAll(computeDeviations(model1, model2, ontologyId1, ontologyId2, categoryName,
									variableNames, category1, category2, mappings));
						}
					}
				}
			}
		}

		SparqlEntityManager.insert(deviations, this.getResultModel());
	}

	/**
	 * Computes the deviations of two models.
	 * 
	 * @param model1        the first model to process
	 * @param model2        the second model to process
	 * @param ontologyId1   the ontology id of the first model
	 * @param ontologyId2   the ontology id of the second model
	 * @param categoryName
	 * @param variableNames
	 * @param category1
	 * @param category2
	 * @param mappings      the given mappings, resources may not belong to the
	 *                      given ontologies
	 * @return the computed deviations
	 * @throws Exception
	 */
	public abstract Collection<Deviation> computeDeviations(Model model1, Model model2, UUID ontologyId1,
			UUID ontologyId2, String categoryName, Collection<String> variableNames, Category category1,
			Category category2, Map<Resource, Set<Resource>> mappings) throws Exception;

}
