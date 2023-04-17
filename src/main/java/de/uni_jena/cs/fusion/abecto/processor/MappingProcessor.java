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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.rulesys.FBRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public abstract class MappingProcessor<P extends Processor<P>> extends Processor<P> {

	private final static List<Rule> correspondenceRules = Arrays.asList(
			// corresponds inverse
			Rule.parseRule("[ (?A " + AV.correspondsToResource + " ?B) "//
					+ "-> (?B " + AV.correspondsToResource + " ?A) ]"),
			// corresponds transitivity
			Rule.parseRule("[ (?A " + AV.correspondsToResource + " ?B),(?B " + AV.correspondsToResource + " ?C) "//
					+ "-> (?A " + AV.correspondsToResource + " ?C) ]"),
			// correspondsNot inverse
			Rule.parseRule("[ (?A " + AV.correspondsNotToResource + " ?B) "//
					+ "-> (?B " + AV.correspondsNotToResource + " ?A) ]"),
			// correspondsNot-corresponds chains
			Rule.parseRule("[ (?A " + AV.correspondsNotToResource + " ?B),(?B " + AV.correspondsToResource + " ?C) "//
					+ "-> (?A " + AV.correspondsNotToResource + " ?C) ]"));

	private static void addIfAbsent(Model model, Resource s, Property p, RDFNode o) {
		if (!model.contains(s, p, o)) {
			model.add(s, p, o);
		}
	}

	/**
	 * Adds the implicit correspondences explicitly to the given {@link Model}.
	 * 
	 * @param model the {@link Model} to extend
	 * @return the extended {@link Model}
	 */
	public static Model inferTransitiveCorrespondences(Model model) {
		InfModel infModel = ModelFactory.createInfModel(new FBRuleReasoner(correspondenceRules), model);
		model.add(infModel.getDeductionsModel());
		return model;
	}

	private Model cachedCorrespondencesModel;

	/**
	 * Adds correspondences of several resources affecting a certain aspect and
	 * thereby transitive implied correspondence. If the correspondences are already
	 * known or contradict an existing incorrespondence, the correspondences will be
	 * discarded silently.
	 *
	 * @param resources the corresponding resources
	 */
	public void addCorrespondence(Collection<Resource> resources, Resource... moreResources) {
		Resource[] allResource = new Resource[resources.size() + moreResources.length];
		resources.toArray(allResource);
		System.arraycopy(moreResources, 0, allResource, resources.size(), moreResources.length);
		addCorrespondence(allResource);
	}

	/**
	 * Add correspondences of several resources belonging to a certain aspect. If
	 * all correspondences are already known or any correspondence contradict an
	 * existing incorrespondence, all correspondences will be discarded silently.
	 *
	 * @param resources the corresponding resources
	 */
	public void addCorrespondence(Resource... resources) {
		if (resources.length < 2) {
			return;
		}
		Model correspondencesModel = getCorrespondencesModel();
		if (!anyIncorrespondend(resources) && !allCorrespondend(resources)) {
			for (int i = 1; i < resources.length; i++) {
				addIfAbsent(correspondencesModel, resources[0], AV.correspondsToResource, resources[i]);
			}
		}
	}

	/**
	 * Adds incorrespondences of resources to one resource affecting a certain
	 * aspect. If the incorrespondence is already known or contradicts an existing
	 * correspondence, the correspondence will be discarded silently.
	 *
	 * @param resource                 first resource
	 * @param incorrespondentResources resources not corresponding to the first
	 *                                 resource
	 */
	public void addIncorrespondence(Resource resource, Resource... incorrespondentResources) {
		Model correspondencesModel = getCorrespondencesModel();
		for (Resource incorrespondentResource : incorrespondentResources) {
			if (!correspondentOrIncorrespondent(resource, incorrespondentResource)) {
				addIfAbsent(correspondencesModel, resource, AV.correspondsNotToResource, incorrespondentResource);
			}
		}
	}

	@Override
	public Model getCorrespondencesModel() {
		if (this.cachedCorrespondencesModel == null) {
			this.cachedCorrespondencesModel = ModelFactory.createInfModel(new FBRuleReasoner(correspondenceRules),
					super.getCorrespondencesModel());
		}
		return this.cachedCorrespondencesModel;
	}

	/**
	 * Determine the corresponding resources of two given datasets.
	 * 
	 * @param dataset1 first dataset to determine corresponding resources for
	 * @param dataset2 second dataset to determine corresponding resources for
	 */
	public abstract void mapDatasets(Resource dataset1, Resource dataset2);

	public void persistTransitiveCorrespondences() {
		this.getOutputMetaModel(null).add(((InfModel) this.getCorrespondencesModel()).getDeductionsModel());
	}

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
		this.persistTransitiveCorrespondences();
	}
}
