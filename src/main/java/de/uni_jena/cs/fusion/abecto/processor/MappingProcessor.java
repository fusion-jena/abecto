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

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;

public abstract class MappingProcessor<P extends Processor<P>> extends Processor<P> {

	/**
	 * Checks if a correspondence or incorrespondence for two given resources in a
	 * given aspect already exist.
	 * <p>
	 * <strong>Note:<strong> The use of this method is not mandatory, as it will
	 * also be checked by {@link #addCorrespondence(Resource, Resource, Aspect)} and
	 * {@link #addIncorrespondence(Resource, Resource, Aspect)}.
	 * 
	 * @param resource1 first resource to check
	 * @param resource2 second resource to check
	 * @param aspect1   aspect affected by the (in)correspondence
	 * @return
	 */
	public final boolean correspondentOrIncorrespondent(Resource resource1, Resource resource2) {
		return Correspondences.correspondentOrIncorrespondent(resource1, resource2, this.getMetaModelUnion(null));
	}

	/**
	 * Adds a correspondence of two resources belonging to a certain aspect and
	 * thereby transitive implied correspondence. If the correspondence is already
	 * known or contradicts an existing incorrespondence, the correspondence will be
	 * discard silently.
	 * 
	 * @param aspect    aspect affected by the correspondence
	 * @param resource1 first corresponding resource
	 * @param resource2 second corresponding resource
	 */
	public final void addCorrespondence(Resource aspect, Resource... resources) {
		Correspondences.addCorrespondence(this.getMetaModelUnion(null), this.getOutputMetaModel(null), aspect,
				resources);
	}

	/**
	 * Adds an incorrespondence of two resources affecting a certain aspect and
	 * thereby transitive implied incorrespondence. If the incorrespondence is
	 * already known or contradicts an existing correspondence, the correspondence
	 * will be discard silently.
	 * 
	 * @param resource1 first corresponding resource
	 * @param resource2 second corresponding resource
	 * @param aspect    aspect affected by the correspondence
	 */
	public final void addIncorrespondence(Aspect aspect, Resource resource1, Resource resource2) {
		Correspondences.addIncorrespondence(this.getMetaModelUnion(null), this.getOutputMetaModel(null),
				aspect.getIri(), resource1, resource2);
	}

	/**
	 * Determine the corresponding resources of two given datasets.
	 * 
	 * @param dataset1 first dataset to determine corresponding resources for
	 * @param dataset2 second dataset to determine corresponding resources for
	 */
	public abstract void mapDatasets(Resource dataset1, Resource dataset2);

	@Override
	public void run() {
		for (Resource dataset1 : this.getDatasets()) {
			for (Resource dataset2 : this.getDatasets()) {
				// do not use Resource#getURI() as it might be null for blank nodes
				if (dataset1.hashCode() < dataset2.hashCode()) { // do not do work twice
					this.mapDatasets(dataset1, dataset2);
				}
			}
		}
	}
}
