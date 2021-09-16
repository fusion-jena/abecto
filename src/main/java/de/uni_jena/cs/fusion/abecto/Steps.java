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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;

public class Steps {

	static Map<Resource, Set<Resource>> getPredecessors(Model configurationModel, Resource plan) {
		// init predecessor map
		Map<Resource, Set<Resource>> stepPredecessors = new HashMap<>();
		// get steps and predecessors
		configurationModel.listSubjectsWithProperty(PPlan.isStepOfPlan, plan).forEach(
				step -> stepPredecessors.put(step, configurationModel.listObjectsOfProperty(step, PPlan.isPrecededBy)
						.filterKeep(RDFNode::isResource).mapWith(RDFNode::asResource).toSet()));
		// get transitive predecessors
		for (Resource step : stepPredecessors.keySet()) {
			Queue<Resource> queue = new LinkedList<Resource>(stepPredecessors.get(step));
			while (!queue.isEmpty()) {
				Resource predecessor = queue.poll();
				stepPredecessors.get(step).add(predecessor);
				queue.addAll(stepPredecessors.get(predecessor));
			}
		}
		return stepPredecessors;
	}
}
