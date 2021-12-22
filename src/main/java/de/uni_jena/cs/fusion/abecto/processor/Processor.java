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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.rulesys.FBRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.PathFactory;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Vocabularies;
import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.Queries;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

/**
 * Provides an abstraction of step processors that generate new primary data or
 * meta data graphs based on its input primary data, meta data graphs and
 * processor parameters.
 */
public abstract class Processor<P extends Processor<P>> implements Runnable {

	// TODO add init and processing state with enabled/disabled input model updates

	private static final Var RESOURCE_1 = Var.alloc("resource1");
	private static final Var RESOURCE_2 = Var.alloc("resource2");
	private static final Var RESOURCE_3 = Var.alloc("resource3");
	private static ExprFactory exprFactory = new ExprFactory();
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
	private Map<Resource, Collection<Model>> inputMetaModelsByDataset = new HashMap<>();

	private Map<Resource, Collection<Model>> inputPrimaryModelsByDataset = new HashMap<>();

	private Map<Resource, Model> outputMetaModelsByDataset = new HashMap<>();

	private Optional<Resource> associatedDataset = Optional.empty();

	/**
	 * The primary data output model of this {@link Processor}. Might be replaces
	 * during processing using {@link #replaceOutputPrimaryModel(Model)}.
	 */
	private Optional<Model> outputPrimaryModel = Optional.empty();
	private Map<Resource, Aspect> aspects = new HashMap<>();

	private Map<Resource, Model> cachedInputMetaModelUnionByDataset = new HashMap<>();

	private Map<Resource, Model> cachedInputPrimaryModelUnionByDataset = new HashMap<>();

	private Map<Resource, Model> cachedMetaModelUnionByDataset = new HashMap<>();

	private File relativeBasePath;

	private Model cachedTransitiveCorrespondencesModel;

	public P addAspects(Aspect... aspects) {
		for (Aspect aspect : aspects) {
			this.aspects.put(aspect.getIri(), aspect);
		}
		return self();
	}

	/**
	 * Adds correspondences of several resources affecting a certain aspect and
	 * thereby transitive implied correspondence. If the correspondences are already
	 * known or contradict an existing incorrespondence, the correspondences will be
	 * discard silently.
	 * 
	 * @param resources the corresponding resources
	 * @param aspect    aspect affected by the correspondence
	 */
	public void addCorrespondence(Resource aspect, Collection<Resource> resources) {
		addCorrespondence(aspect, resources.toArray(l -> new Resource[l]));
	}

	/**
	 * Add correspondences of several resources belonging to a certain aspect. If
	 * all correspondences are already known or any correspondence contradict an
	 * existing incorrespondence, all correspondences will be discard silently.
	 * 
	 * @param resources the corresponding resources
	 * @param aspect    aspect the corresponding resources belong to
	 */
	public void addCorrespondence(Resource aspect, Resource... resources) {
		if (resources.length < 2) {
			return;
		}
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		if (!anyIncorrespondend(resources) && !allCorrespondend(resources)) {
			Query query = new ConstructBuilder()//
					.addConstruct(aspect, AV.relevantResource, resources[0])//
					.addConstruct(aspect, AV.relevantResource, RESOURCE_2)//
					.addConstruct(resources[0], AV.correspondsToResource, RESOURCE_2)//
					.addWhereValueVar(RESOURCE_2, (Object[]) Arrays.copyOfRange(resources, 1, resources.length))
					.build();
			QueryExecutionFactory.create(query, transitiveCorrespondencesModel)
					.execConstruct(transitiveCorrespondencesModel);
		}
	}

	/**
	 * Adds incorrespondences of resources to one resource affecting a certain
	 * aspect. If the incorrespondence is already known or contradicts an existing
	 * correspondence, the correspondence will be discard silently.
	 * 
	 * @param aspect                   aspect affected by the incorrespondence
	 * @param resource                 first resource
	 * @param incorrespondentResources resources not corresponding to the first
	 *                                 resource
	 */
	public void addIncorrespondence(Resource aspect, Resource resource, Resource... incorrespondentResources) {
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		for (Resource incorrespondentResource : incorrespondentResources) {
			if (!correspondentOrIncorrespondent(resource, incorrespondentResource)) {
				Query query = new ConstructBuilder()//
						.addConstruct(aspect, AV.relevantResource, resource)//
						.addConstruct(aspect, AV.relevantResource, incorrespondentResource)//
						.addConstruct(resource, AV.correspondsNotToResource, incorrespondentResource)//
						.build();
				QueryExecutionFactory.create(query, transitiveCorrespondencesModel)
						.execConstruct(transitiveCorrespondencesModel);
			}
		}
	}

	public final P addInputMetaModel(Resource dataset, Model inputMetaModel) {
		this.cachedInputMetaModelUnionByDataset.remove(dataset);
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.inputMetaModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).add(inputMetaModel);
		return self();
	}

	public final P addInputPrimaryModel(Resource dataset, Model inputPrimaryModel) {
		this.cachedInputPrimaryModelUnionByDataset.remove(dataset);
		this.inputPrimaryModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).add(inputPrimaryModel);
		return self();
	}

	public final P addInputProcessor(Processor<?> inputProcessor) {
		inputProcessor.inputMetaModelsByDataset.forEach((d, ms) -> ms.forEach(m -> this.addInputMetaModel(d, m)));
		inputProcessor.outputMetaModelsByDataset.forEach((d, m) -> this.addInputMetaModel(d, m));
		inputProcessor.inputPrimaryModelsByDataset.forEach((d, ms) -> ms.forEach(m -> this.addInputPrimaryModel(d, m)));
		if (inputProcessor.associatedDataset.isPresent() && inputProcessor.outputPrimaryModel.isPresent()) {
			this.addInputPrimaryModel(inputProcessor.associatedDataset.get(), inputProcessor.outputPrimaryModel.get());
		}
		return self();
	}

	/**
	 * Checks if all given resources correspond to each other.
	 * 
	 * @param resources the resources to check
	 * @return {@code true}, if all resources correspond to each other, otherwise
	 *         {@code false}
	 */
	public boolean allCorrespondend(Resource... resources) {
		if (resources.length < 2) {
			return true;
		}
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		Query query = new AskBuilder()//
				// TODO replace multiple VALUES clauses workaround when a better solution exists
				// NOTE: addWhereValueVar() does not cross-join / add multiple VALUES clauses
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_1).addWhereValueVar(RESOURCE_1, (Object[]) resources))
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_2).addWhereValueVar(RESOURCE_2, (Object[]) resources))
				.addFilter(exprFactory.ne(RESOURCE_1, RESOURCE_2))
				.addFilter(exprFactory
						.notexists(new WhereBuilder().addWhere(RESOURCE_1, AV.correspondsToResource, RESOURCE_2)))
				.build();
		return !QueryExecutionFactory.create(query, transitiveCorrespondencesModel).execAsk();
	}

	/**
	 * Checks if any pair of the given resources is known to be incorrespondent.
	 * 
	 * @param resources the resources to check
	 * @return {@code true}, if all resources are not incorrespondent to each other,
	 *         otherwise {@code false}
	 */
	public boolean anyIncorrespondend(Resource... resources) {
		if (resources.length < 2) {
			return false;
		}
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		Query query = new AskBuilder()//
				// TODO replace multiple VALUES clauses workaround when a better solution exists
				// NOTE: addWhereValueVar() does not cross-join / add multiple VALUES clauses
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_1).addWhereValueVar(RESOURCE_1, (Object[]) resources))
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_2).addWhereValueVar(RESOURCE_2, (Object[]) resources))
				.addFilter(exprFactory.ne(RESOURCE_1, RESOURCE_2))
				.addWhere(RESOURCE_1, AV.correspondsNotToResource, RESOURCE_2).build();
		return QueryExecutionFactory.create(query, transitiveCorrespondencesModel).execAsk();
	}

	/**
	 * Checks if a correspondence for two given resources in a given aspect exists.
	 * 
	 * @param resource1 first resource to check
	 * @param resource2 second resource to check
	 * @param aspect1   aspect affected by the correspondence
	 * @return {@code true}, if resources correspond to each, otherwise
	 *         {@code false}
	 */
	public boolean correspond(Resource resource1, Resource resource2) {
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		Query query = new AskBuilder().addWhere(resource1, AV.correspondsToResource, resource2).build();
		return resource1.equals(resource2)
				|| QueryExecutionFactory.create(query, transitiveCorrespondencesModel).execAsk();
	}

	/**
	 * Checks if a correspondence or incorrespondence for two given resources in a
	 * given aspect already exist.
	 * <p>
	 * <strong>Note:<strong> The use of this method is not mandatory, as it will
	 * also be called by {@link #addCorrespondence(Resource, Resource, Aspect)} and
	 * {@link #addIncorrespondence(Resource, Resource, Aspect)}.
	 * 
	 * @param resource1 first resource to check
	 * @param resource2 second resource to check
	 * @return {@code true}, if an equivalent or contradicting (in)correspondence
	 *         exists, otherwise {@code false}
	 */
	public boolean correspondentOrIncorrespondent(Resource resource1, Resource resource2) {
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		Query query = new AskBuilder().addWhere(resource1, AV.correspondsToResource, resource2)
				.addUnion(new WhereBuilder().addWhere(resource1, AV.correspondsNotToResource, resource2)).build();
		return resource1.equals(resource2)
				|| QueryExecutionFactory.create(query, transitiveCorrespondencesModel).execAsk();
	}

	public Map<Resource, Aspect> getAspects() {
		return aspects;
	}

	public final Optional<Resource> getAssociatedDataset() {
		return this.associatedDataset;
	}

	/**
	 * Returns {@link List Lists} of {@link Resource Resources} that belong to a
	 * given aspect and correspond to each other.
	 * 
	 * @param aspect the aspect all returned {@link Resource Resources} must belong
	 *               to
	 * @return the {@link List Lists} of corresponding {@link Resource Resources}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Stream<List<Resource>> getCorrespondenceSets(Resource aspect) {
		Model transitiveCorrespondencesModel = getTransitiveCorrespondencesModel();
		Query query = new SelectBuilder().addVar(RESOURCE_1).addVar(RESOURCE_2)
				.addWhere(aspect, AV.relevantResource, RESOURCE_1)//
				.addWhere(aspect, AV.relevantResource, RESOURCE_2)//
				.addWhere(new TriplePath(RESOURCE_1,
						PathFactory.pathZeroOrOne(PathFactory.pathLink(AV.correspondsToResource.asNode())), RESOURCE_2))
				.addFilter(exprFactory
						.notexists(new WhereBuilder().addWhere(RESOURCE_1, AV.correspondsToResource, RESOURCE_3)
								.addFilter(exprFactory.gt(exprFactory.str(RESOURCE_1), exprFactory.str(RESOURCE_3)))))
				.addOrderBy(RESOURCE_1).build();
		return Queries.getStreamOfResultsGroupedBy(transitiveCorrespondencesModel, query, RESOURCE_1.getName())
				.map(m -> (List<Resource>) (List) m.get(RESOURCE_2.getName()));
	}

	public final Set<Resource> getDatasets() {
		Set<Resource> inputDatasets = new HashSet<>();
		inputDatasets.addAll(this.inputMetaModelsByDataset.keySet());
		inputDatasets.addAll(this.inputPrimaryModelsByDataset.keySet());
		this.associatedDataset.ifPresent(inputDatasets::add);
		inputDatasets.remove(null);
		return inputDatasets;
	}

	public final Model getInputMetaModelUnion() {
		return Models.union(this.inputMetaModelsByDataset.values().stream().flatMap(Collection::stream));
	}

	public final Model getInputMetaModelUnion(Resource dataset) {
		return cachedInputMetaModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(this.inputMetaModelsByDataset.get(dataset)));
	}
	public final Model getInputPrimaryModelUnion(Resource dataset) {
		return cachedInputPrimaryModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(this.inputPrimaryModelsByDataset.get(dataset)));
	}
	/**
	 * Returns a union of the input meta models and the result meta model of a
	 * dataset, or of the general meta models, if {@code dataset} is {@code null}.
	 * 
	 * @param dataset the assigned dataset the united meta models or {@code null}
	 *                for the general meta models.
	 * @return union of the meta models
	 */
	public Model getMetaModelUnion(@Nullable Resource dataset) {
		return cachedMetaModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(getOutputMetaModel(dataset), this.inputMetaModelsByDataset.get(dataset)));
	}
	/**
	 * Returns the output meta model of a dataset, or the general output meta model,
	 * if {@code dataset} is {@code null}. If not present, a new model will be
	 * created.
	 * 
	 * @param datasetthe assigned dataset the output meta models or {@code null} for
	 *                   the general meta models.
	 * @return the output meta model
	 */
	public final Model getOutputMetaModel(@Nullable Resource dataset) {
		if (!outputMetaModelsByDataset.containsKey(dataset)) {
			this.cachedMetaModelUnionByDataset.remove(dataset);
		}
		return this.outputMetaModelsByDataset.computeIfAbsent(dataset,
				k -> ModelFactory.createDefaultModel().withDefaultMappings(Vocabularies.getDefaultPrefixMapping()));
	}

	/**
	 * Returns the primary data output model of this {@link Processor}. The model
	 * can be replaces during processing using
	 * {@link #replaceOutputPrimaryModel(Model)}.
	 * 
	 * @return the primary data output model
	 */
	public final Optional<Model> getOutputPrimaryModel() {
		return this.outputPrimaryModel;
	}

	public Model getPrimaryModelUnion() {
		return Models.union(
				this.getOutputPrimaryModel().orElseThrow(() -> new IllegalStateException("No output primary model .")),
				this.inputPrimaryModelsByDataset.get(
						this.associatedDataset.orElseThrow(() -> new IllegalStateException("No associated dataset."))));
	}

	public File getRelativeBasePath() {
		return relativeBasePath;
	}

	private Model getTransitiveCorrespondencesModel() {
		if (this.cachedTransitiveCorrespondencesModel == null) {
			this.cachedTransitiveCorrespondencesModel = ModelFactory
					.createInfModel(new FBRuleReasoner(correspondenceRules), this.getMetaModelUnion(null));
		}
		return this.cachedTransitiveCorrespondencesModel;
	}

	public void removeEmptyModels() {
		if (outputPrimaryModel.isPresent() && outputPrimaryModel.get().isEmpty()) {
			outputPrimaryModel = Optional.empty();
		}
		for (Resource dataset : new ArrayList<>(outputMetaModelsByDataset.keySet())) {
			if (outputMetaModelsByDataset.get(dataset).isEmpty()) {
				outputMetaModelsByDataset.remove(dataset);
			}
		}
	}

	protected final P replaceOutputMetaModel(Resource dataset, Model outputMetaModel) {
		Objects.requireNonNull(dataset, "Replacing general output meta model not permitted.");
		// Note: otherwise cachedTransitiveCorrespondencesModel would get invalid
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.outputMetaModelsByDataset.put(dataset, outputMetaModel);
		return self();
	}

	/**
	 * Replaces the current output primary model.
	 * <p>
	 * This enables the use of none in-memory models by source processors. Data
	 * added to the current output primary model will get lost.
	 * 
	 * @param outputPrimaryModel the model that replaces the current output primary
	 *                           model
	 */
	protected final P replaceOutputPrimaryModel(Model outputPrimaryModel) {
		if (this.associatedDataset.isEmpty()) {
			throw new IllegalStateException("Operation only permited, if step is associated with a dataset.");
		}
		this.outputPrimaryModel = Optional.of(outputPrimaryModel);
		return self();
	}

	@SuppressWarnings("unchecked")
	private P self() {
		return (P) this;
	}

	/**
	 * Sets the associated dataset of the processor, which is the dataset the output
	 * primary model will belong to.
	 * 
	 * @param dataset
	 */
	public final P setAssociatedDataset(Resource dataset) {
		this.associatedDataset = Optional.of(dataset);
		this.outputPrimaryModel = Optional.of(ModelFactory.createDefaultModel());
		return self();
	}

	public void setRelativeBasePath(File relativeBasePath) {
		this.relativeBasePath = relativeBasePath;
	}
}
