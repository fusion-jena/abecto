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
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;

public class Plans {
	static Resource getPlan(Model configurationModel, @Nullable String planIri) {
		if (planIri != null) {
			Resource plan = ResourceFactory.createResource(planIri);
			if (!configurationModel.contains(plan, RDF.type, PPlan.Plan)) {
				throw new IllegalArgumentException(
						String.format("Selected plan %s not contained in the configuration.", planIri));
			}
			return plan;
		} else {
			try {
				return Models.assertOne(configurationModel.listSubjectsWithProperty(RDF.type, PPlan.Plan));
			} catch (NoSuchElementException e) {
				throw new IllegalArgumentException(String.format("Configuration does not contain a plan."));
			} catch (ToManyElementsException e) {
				throw new IllegalArgumentException(
						String.format("Configuration contains more than one plan, but no plan was selected."));
			}
		}
	}

	static Map<Resource, Set<Resource>> getStepPredecessors(Model configurationModel, Resource plan)
			throws NoSuchElementException {
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
				if (stepPredecessors.containsKey(predecessor)) {
					queue.addAll(stepPredecessors.get(predecessor));
				} else {
					throw new NoSuchElementException(String.format("Prodecessor %s not defined.", predecessor));
				}
			}
		}
		return stepPredecessors;
	}
}
