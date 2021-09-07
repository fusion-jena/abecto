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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.processor.AbstractValueComparisonProcessor;
import de.uni_jena.cs.fusion.abecto.util.Correspondences;

public class ResourceValueComparisonProcessor extends AbstractValueComparisonProcessor {

	private Model mappingModel;

	@Override
	public boolean isValidValue(String variable, Resource dataset, Resource resource, RDFNode value) {
		if (!value.isResource()) {
			// TODO create issue
			return false;
		}
		return true;
	}

	@Override
	public boolean equivalentValues(RDFNode value1, RDFNode value2) {
		if (this.mappingModel == null) {
			this.mappingModel = this.getInputMetaModelUnion(null);
		}
		return Correspondences.correspond(value1.asResource(), value2.asResource(), this.mappingModel);
	}
}
