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
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public interface RefinementProcessor<P extends ParameterModel> extends Processor<P> {

	/**
	 * Add a {@link Model} to process.
	 * 
	 * @param inputModel {@link Model}s to process
	 */
	public void addInputModelGroup(UUID uuid, Collection<Model> inputModelGroup);

	/**
	 * Add {@link Model}s to process.
	 * 
	 * @param inputModelGroups {@link Model}s to process
	 */
	default public void addInputModelGroups(Map<UUID, Collection<Model>> inputModelGroups) {
		for (Entry<UUID, Collection<Model>> inputModelGroup : inputModelGroups.entrySet()) {
			this.addInputModelGroup(inputModelGroup.getKey(), inputModelGroup.getValue());
		}
	}

	/**
	 * Add a {@link Processor} this {@link Processor} depends on.
	 * 
	 * @param processor {@link Processor} this {@link Processor} depends on
	 */
	public void addInputProcessor(Processor<?> processor);

	/**
	 * Add the previous meta {@link Model}.
	 * 
	 * @param metaModel previous meta {@link Model}
	 */
	public void addMetaModels(Collection<Model> metaModels);
}
