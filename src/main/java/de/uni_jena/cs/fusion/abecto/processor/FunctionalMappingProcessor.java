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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;

/**
 * Provides correspondences based on functional (n:1 or 1:1) variables between aspect.
 * <p>
 * <strong>Example:</strong> If resource R1 of dataset D1 and aspect A1 refers
 * with variable V1 to resource R2 of dataset D1 and aspect A2, and resource R3
 * of dataset D2 and aspect A1 refers with variable V1 to resource R4 of dataset
 * D2 and aspect A2, and R1 corresponds to R3, then R2 corresponds to R4.
 *
 */
public class FunctionalMappingProcessor extends MappingProcessor<FunctionalMappingProcessor> {

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
		getCorrespondenceGroups(referringAspect.getIri()).forEach(referringResources -> {
			Collection<Resource> referredResources = new ArrayList<>();
			for (Resource referringResource : referringResources) {
				for (Resource dataset : this.getDatasets()) {
					Optional<Map<String, Set<RDFNode>>> referringResourceValues = Aspect.getResource(referringAspect,
							dataset, referringResource, this.getInputPrimaryModelUnion(dataset));
					if (referringResourceValues.isPresent()
							&& referringResourceValues.get().containsKey(referringVariable)) {
						for (RDFNode referredResource : referringResourceValues.get().get(referringVariable)) {
							if (referredResource.isResource()) {
								referredResources.add(referredResource.asResource());
							} else {
								// report invalid value
								Metadata.addIssue(referringResource, referringVariable, referredResource,
										referringAspect.getIri(), "Invalid Value", "Should be a resource.",
										this.getOutputMetaModel(dataset));
							}
						}
					}
				}
			}
			addCorrespondence(this.referredAspect, referredResources);
		});
	}
}
