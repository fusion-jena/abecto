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

public abstract class MappingProcessor<P extends Processor<P>> extends Processor<P> {

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
