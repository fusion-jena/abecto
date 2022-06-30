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

import org.apache.jena.rdf.model.RDFNode;

public class ResourceValueComparisonProcessor
		extends AbstractValueComparisonProcessor<ResourceValueComparisonProcessor> {

	@Override
	public boolean isValidValue(RDFNode value) {
		if (!value.isResource()) {
			return false;
		}
		return true;
	}

	@Override
	public String invalidValueComment() {
		return "Should be a resource.";
	}

	@Override
	public boolean equivalentValues(RDFNode value1, RDFNode value2) {
		return correspond(value1.asResource(), value2.asResource());
	}
}
