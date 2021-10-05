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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;

/**
 * Provides an abstraction of step processors that generate new primary data or
 * meta data graphs based on its input primary data, meta data graphs and
 * processor parameters.
 */
public abstract class Processor<P extends Processor<P>> implements Runnable {

	private Map<String, List<Object>> parameter = new HashMap<>();
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

	public final P addInputMetaModels(Resource dataset, Collection<Model> inputMetaModels) {
		this.cachedInputMetaModelUnionByDataset.remove(dataset);
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.inputMetaModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).addAll(inputMetaModels);
		return self();
	}

	public final P addInputPrimaryModels(Resource dataset, Collection<Model> inputPrimaryModels) {
		this.cachedInputPrimaryModelUnionByDataset.remove(dataset);
		this.inputPrimaryModelsByDataset.computeIfAbsent(dataset, d -> new HashSet<>()).addAll(inputPrimaryModels);
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

	public final Set<Resource> getInputDatasets() {
		Set<Resource> inputDatasets = new HashSet<>();
		inputDatasets.addAll(this.inputMetaModelsByDataset.keySet());
		inputDatasets.addAll(this.inputPrimaryModelsByDataset.keySet());
		inputDatasets.remove(null);
		return inputDatasets;
	}

	public final Model getInputMetaModelUnion() {
		return Models.union(this.inputMetaModelsByDataset.values().stream().flatMap(Collection::stream));
	}

	private Map<Resource, Model> cachedInputMetaModelUnionByDataset = new HashMap<>();

	public final Model getInputMetaModelUnion(Resource dataset) {
		return cachedInputMetaModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(this.inputMetaModelsByDataset.get(dataset)));
	}

	private Map<Resource, Model> cachedInputPrimaryModelUnionByDataset = new HashMap<>();

	public final Model getInputPrimaryModelUnion(Resource dataset) {
		return cachedInputPrimaryModelUnionByDataset.computeIfAbsent(dataset,
				d -> Models.union(this.inputPrimaryModelsByDataset.get(dataset)));
	}

	public Model getMetaModelUnion() {
		return Models.union(this.getInputMetaModelUnion(), this.getOutputMetaModelUnion());
	}

	private Map<Resource, Model> cachedMetaModelUnionByDataset = new HashMap<>();

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

	/**
	 * Returns the parameter values for a given key asserting a given type.
	 * 
	 * @param <T>         type of the returned value
	 * @param key         key of the parameter
	 * @param resultClass class object of the type of the returned values
	 * @return values of the parameter
	 * @throws ClassCastException   if the parameter values do not match the
	 *                              requested type
	 * @throws NullPointerException if the parameter has not been set
	 */
	@SuppressWarnings("unchecked")
	public final <T> List<T> getParameterValues(String key, Class<T> resultClass)
			throws ClassCastException, NullPointerException {
		return (List<T>) Objects.requireNonNull(this.parameter.get(key));
	}

	/**
	 * Returns the parameter values for a given key asserting a given type, if
	 * present, otherwise returns {@code other}.
	 * 
	 * @param <T>   type of the returned value
	 * @param key   key of the parameter
	 * @param other values to be returned, if the parameter has not been set
	 * @return values of the parameter
	 * @throws ClassCastException if the parameter values do not match the requested
	 *                            type
	 */
	@SuppressWarnings("unchecked")
	public final <T> List<T> getParameterValues(String key, @Nonnull List<T> other)
			throws ClassCastException, NullPointerException {
		return (List<T>) Objects.requireNonNull(this.parameter.getOrDefault(key, (List<Object>) other));
	}

	/**
	 * Returns the parameter value for a given key asserting a given type.
	 * 
	 * @param <T>         type of the returned value
	 * @param key         key of the parameter
	 * @param resultClass class object of the type of the returned value
	 * @return value of the parameter
	 * @throws ClassCastException      if the parameter value does not match the
	 *                                 requested type
	 * @throws NullPointerException    if the parameter has not been set
	 * @throws ToManyElementsException if the parameter has more than one value
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getParameterValue(String key, Class<T> resultClass)
			throws ClassCastException, NullPointerException {
		List<T> values = (List<T>) Objects.requireNonNull(this.parameter.get(key));
		switch (values.size()) {
		case 0:
			throw new NullPointerException();
		case 1:
			return values.get(0);
		default:
			throw new ToManyElementsException();
		}
	}

	/**
	 * Returns the parameter value for a given key asserting a given type.
	 * 
	 * @param <T>         type of the returned value
	 * @param key         key of the parameter
	 * @param resultClass class object of the type of the returned value
	 * @return value of the parameter
	 * @throws ClassCastException      if the parameter value does not match the
	 *                                 requested type
	 * @throws ToManyElementsException if the parameter has more than one value
	 */
	@SuppressWarnings("unchecked")
	public final <T> Optional<T> getParameterValueOptional(String key, Class<T> resultClass)
			throws ClassCastException, NullPointerException {
		List<T> values = (List<T>) Objects.requireNonNull(this.parameter.get(key));
		switch (values.size()) {
		case 0:
			return Optional.empty();
		case 1:
			return Optional.of(values.get(0));
		default:
			throw new ToManyElementsException();
		}
	}

	/**
	 * Returns the parameter value for a given key asserting a given type, if
	 * present, otherwise returns {@code other}.
	 * 
	 * @param <T>   type of the returned value
	 * @param key   key of the parameter
	 * @param other value to be returned, if the parameter has not been set
	 * @return value of the parameter
	 * @throws ClassCastException      if the parameter value does not match the
	 *                                 requested type
	 * @throws ToManyElementsException if the parameter has more than one value
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getParameterValue(String key, @Nonnull T other) throws ClassCastException, NullPointerException {
		Objects.requireNonNull(other);
		List<T> values = (List<T>) Objects.requireNonNull(this.parameter.get(key));
		switch (values.size()) {
		case 0:
			throw new NullPointerException();
		case 1:
			return values.get(0);
		default:
			throw new ToManyElementsException();
		}
	}

	public Model getPrimaryModelUnion() {
		return Models.union(
				this.inputPrimaryModelsByDataset.get(
						this.associatedDataset.orElseThrow(() -> new IllegalStateException("No associated dataset."))),
				this.getOutputPrimaryModel().orElseThrow(() -> new IllegalStateException("No output primary model .")));
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

	public P setAspectMap(Map<Resource, Aspect> aspects) {
		this.aspects = aspects;
		return self();
	}

	public final P setOutputMetaModel(@Nullable Resource dataset, Model outputMetaModel) {
		this.cachedMetaModelUnionByDataset.remove(dataset);
		this.outputMetaModelsByDataset.put(dataset, outputMetaModel);
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
	public final P replaceOutputPrimaryModel(Model outputPrimaryModel) {
		if (this.associatedDataset.isEmpty()) {
			throw new IllegalStateException("Operation only permited, if step is associated with a dataset.");
		}
		this.outputPrimaryModel = Optional.of(outputPrimaryModel);
		return self();
	}

	/**
	 * Sets the parameter value for a given key.
	 * 
	 * @param key   key of the parameter
	 * @param value value of the parameter
	 */
	public final P setParameterValues(String key, List<Object> value) {
		this.parameter.put(key, value);
		return self();
	}

	@SuppressWarnings("unchecked")
	private P self() {
		return (P) this;
	}
}
