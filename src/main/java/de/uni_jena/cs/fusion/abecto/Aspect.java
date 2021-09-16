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
package de.uni_jena.cs.fusion.abecto;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

public class Aspect {
	public final Resource iri;
	private final String keyVariableName;
	private final Var keyVariable;
	private final Map<Resource, Query> patternByDataset = new HashMap<>();

	public Aspect(Resource iri, String keyVariableName) {
		this.iri = iri;
		this.keyVariableName = keyVariableName;
		this.keyVariable = Var.alloc(keyVariableName);
	}

	public void setPattern(Resource dataset, Query pattern) {
		patternByDataset.put(dataset, pattern);
	}

	public Query getPattern(Resource dataset) {
		return patternByDataset.get(dataset);
	}

	public String getKeyVariableName() {
		return keyVariableName;
	}

	public Var getKeyVariable() {
		return keyVariable;
	}

}
