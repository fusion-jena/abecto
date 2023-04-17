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

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;

public class UsePresentMappingProcessor extends MappingProcessor<UsePresentMappingProcessor> {
	final static Logger log = LoggerFactory.getLogger(UsePresentMappingProcessor.class);

	@Parameter
	public Resource aspect;
	@Parameter
	public String variable;

	@Override
	public void run() {
		Model outputMetaModel = this.getOutputMetaModel(null);
		Aspect aspectObject = Objects.requireNonNull(getAspects().get(this.aspect), "Unknown aspect.");

		// execute query for each dataset
		for (Resource dataset : this.getDatasets()) {
			// check if aspect pattern contains the variable
			try {
				if (!aspectObject.getPattern(dataset).getResultVars().contains(variable)) {
					log.warn("Missing variable \"{}\" in pattern of aspect \"{}\" for dataset \"{}\".", variable,
							aspectObject.getKeyVariableName(), dataset);
					return;
				}
			} catch (NullPointerException e) {
				log.warn("No pattern of aspect \"{}\" for dataset \"{}\" defined.", aspectObject.getKeyVariableName(),
						dataset);
				return;
			}

			String varPath = aspectObject.getVarPathAsString(dataset, variable);

			Model inputPrimaryModel = this.getInputPrimaryModelUnion(dataset);

			for (Entry<RDFNode, Set<Resource>> entry : Aspect
					.getResourceIndex(aspectObject, dataset, Collections.singletonList(variable), inputPrimaryModel)
					.get(variable).entrySet()) {
				RDFNode variableValue = entry.getKey();
				Set<Resource> affectedResources = entry.getValue();
				if (variableValue.isResource()) {
					addCorrespondence(affectedResources, variableValue.asResource());
				} else {
					for (Resource affectedResource : entry.getValue())
						Metadata.addIssue(affectedResource, variable, variableValue, this.aspect, "Invalid Value",
								String.format(
										"Failed to get corresponding resource, found a literal: <%s> %s \"%s\"^^<%s>",
										affectedResource, varPath, variableValue,
										variableValue.asLiteral().getDatatypeURI()),
								outputMetaModel);
				}
			}
		}

		this.persistTransitiveCorrespondences();
	}

	@Override
	public void mapDatasets(Resource dataset1, Resource dataset2) {
		// not used
	}

}
