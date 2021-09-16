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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Aspects;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.Parameter;

public class RelationalMappingProcessor extends MappingProcessor {

	@Parameter
	public Resource referringAspect;
	@Parameter
	public String referringVariable;
	@Parameter
	public Resource referredAspect;

	@Override
	public void mapDatasets(Resource dataset1, Resource dataset2) {
		// do nothing, method not used
	}

	@Override
	public void run() {
		Aspect referringAspect = this.getAspects().get(this.referringAspect);
		Aspect referredAspect = this.getAspects().get(this.referredAspect);
		Correspondences.getCorrespondenceSets(this.getInputMetaModelUnion(null), referringAspect.iri)
				.forEach(referringResources -> {
					Collection<Resource> referredResources = new ArrayList<>();
					for (Resource referringResource : referringResources) {
						for (Resource dataset : this.getInputDatasets()) {
							Optional<Map<String, Set<RDFNode>>> referringResourceValues = Aspects.getResource(
									referringAspect, dataset, referringResource,
									this.getInputPrimaryModelUnion(dataset));
							if (referringResourceValues.isPresent()
									&& referringResourceValues.get().containsKey(referringVariable)) {
								for (RDFNode referredResource : referringResourceValues.get().get(referringVariable)) {
									if (referredResource.isResource()) {
										referredResources.add(referredResource.asResource());
									} else {
										// TODO report issue
									}
								}
							}
						}
					}
					Correspondences.addCorrespondence(this.getMetaModelUnion(null), this.getOutputMetaModel(null),
							referredAspect.iri, referredResources);
				});
	}
}
