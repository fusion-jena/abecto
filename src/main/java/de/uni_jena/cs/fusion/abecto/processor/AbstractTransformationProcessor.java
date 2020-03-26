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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public abstract class AbstractTransformationProcessor<P extends ParameterModel> extends AbstractRefinementProcessor<P>
		implements TransformationProcessor<P> {

	@Override
	public Map<UUID, Collection<Model>> getDataModels() {
		if (!this.isSucceeded()) {
			throw new IllegalStateException("Result Model is not avaliable.");
		}
		Map<UUID, Collection<Model>> result = new HashMap<>();
		for (UUID uuid : this.inputGroupSubModels.keySet()) {
			Collection<Model> collection = result.computeIfAbsent(uuid, (a) -> new HashSet<Model>());
			collection.addAll(this.inputGroupSubModels.get(uuid));
			collection.add(this.getResultModel());
		}
		return result;
	}

	@Override
	public Collection<Model> getMetaModels() {
		return this.metaSubModels;
	}
}
