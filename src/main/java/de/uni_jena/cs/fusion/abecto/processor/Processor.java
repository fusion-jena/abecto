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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Vocabularies;
import de.uni_jena.cs.fusion.abecto.util.Models;

/**
 * Provides an abstraction of step processors that generate new primary data or
 * meta data graphs based on its input primary data, meta data graphs and
 * processor parameters.
 */
public abstract class Processor<P extends Processor<P>> implements Runnable {

	private Map<Resource, Collection<Model>> inputMetaModelsByDataset = new HashMap<>();
	private Map<Resource, Collection<Model>> inputPrimaryModelsByDataset = new HashMap<>();
	private Map<Resource, Model> outputMetaModelsByDataset = new HashMap<>();
	private Optional<Resource> associatedDataset = Optional.empty();
	/**
	 * The primary data output model of this {@link Processor}. Might be replaces
	 * during processing using {@link #replaceOutputPrimaryModel(Model)}.
	 */
	private Optional<Model> outputPrimaryModel = Optional.empty();
	private Map<Resource, Aspect> aspects = Collections.emptyMap();

	private Map<Resource, Model> cachedInputMetaModelUnionByDataset = new HashMap<>();

	private Map<Resource, Model> cachedInputPrimaryModelUnionByDataset = new HashMap<>();

	private Map<Resource, Model> cachedMetaModelUnionByDataset = new HashMap<>();

	public final P addInputMetaModel(Resource dataset, Model inputMetaModel) {
		this.cachedInputMetaModelUnionByDataset.remove(dataset);
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.inputMetaModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).add(inputMetaModel);
		this.initOutputMetaModel(dataset);
		return self();
	}

	public final P addInputMetaModels(Resource dataset, Collection<Model> inputMetaModels) {
		this.cachedInputMetaModelUnionByDataset.remove(dataset);
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.inputMetaModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).addAll(inputMetaModels);
		this.initOutputMetaModel(dataset);
		return self();
	}

	public final P addInputPrimaryModel(Resource dataset, Model inputPrimaryModel) {
		this.cachedInputPrimaryModelUnionByDataset.remove(dataset);
		this.inputPrimaryModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).add(inputPrimaryModel);
		this.initOutputMetaModel(dataset);
		return self();
	}

	public final P addInputPrimaryModels(Resource dataset, Collection<Model> inputPrimaryModels) {
		this.cachedInputPrimaryModelUnionByDataset.remove(dataset);
		this.inputPrimaryModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).addAll(inputPrimaryModels);
		this.initOutputMetaModel(dataset);
		return self();
	}

	public final P addInputProcessor(Processor<?> inputProcessor) {
		inputProcessor.inputMetaModelsByDataset.forEach((d, m) -> this.addInputMetaModels(d, m));
		inputProcessor.outputMetaModelsByDataset
				.forEach((d, m) -> this.addInputMetaModels(d, Collections.singleton(m)));
		inputProcessor.inputPrimaryModelsByDataset.forEach((d, m) -> this.addInputPrimaryModels(d, m));
		if (inputProcessor.associatedDataset.isPresent() && inputProcessor.outputPrimaryModel.isPresent()) {
			this.addInputPrimaryModels(inputProcessor.associatedDataset.get(),
					Collections.singleton(inputProcessor.outputPrimaryModel.get()));
		}
		return self();
	}

	public Map<Resource, Aspect> getAspects() {
		return aspects;
	}

	public final Optional<Resource> getAssociatedDataset() {
		return this.associatedDataset;
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

	public Model getMetaModelUnion() {
		return Models.union(this.getInputMetaModelUnion(), this.getOutputMetaModelUnion());
	}

	/**
	 * Returns a union of the input meta models and the result meta model of a
	 * dataset, or of the general meta models. if {@code dataset} is {@code null}.
	 * 
	 * @param dataset the assigned dataset the united meta models or {@code null}
	 *                for the general meta models.
	 * @return union of the meta models
	 */
	public Model getMetaModelUnion(@Nullable Resource dataset) {
		return cachedMetaModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(this.getInputMetaModelUnion(), this.getOutputMetaModel(d)));
	}

	public final Model getOutputMetaModel(@Nullable Resource dataset) {
		return this.outputMetaModelsByDataset.get(dataset);
	}

	public final Model getOutputMetaModelUnion() {
		return Models.union(this.outputMetaModelsByDataset.values());
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
				this.inputPrimaryModelsByDataset.get(
						this.associatedDataset.orElseThrow(() -> new IllegalStateException("No associated dataset."))),
				this.getOutputPrimaryModel().orElseThrow(() -> new IllegalStateException("No output primary model .")));
	}

	private final P initOutputMetaModel(@Nullable Resource dataset) {
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.outputMetaModelsByDataset.computeIfAbsent(dataset,
				k -> ModelFactory.createDefaultModel().withDefaultMappings(Vocabularies.getDefaultPrefixMapping()));
		return self();
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

	protected final P replaceOutputMetaModel(@Nullable Resource dataset, Model outputMetaModel) {
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

	public P setAspectMap(Map<Resource, Aspect> aspects) {
		this.aspects = aspects;
		this.initOutputMetaModel(null); // assert to be called once
		return self();
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
		this.initOutputMetaModel(dataset);
		this.initOutputMetaModel(null); // assert to be called once
		return self();
	}

}
