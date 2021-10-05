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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;

public abstract class AbstractValueComparisonProcessor<P extends Processor<P>> extends Processor<P> {

	/** Aspect to process. */
	@Parameter
	public Resource aspect;
	/** Variables to process. */
	@Parameter
	public Collection<String> variables;

	@Override
	public final void run() {
		Aspect aspect = this.getAspects().get(this.aspect);
		Correspondences.getCorrespondenceSets(this.getInputMetaModelUnion(null), aspect.getIri())
				.forEach(correspondingResources -> {
					for (Resource correspondingResource1 : correspondingResources) {
						for (Resource dataset1 : this.getInputDatasets()) {
							Optional<Map<String, Set<RDFNode>>> values1 = Aspect.getResource(aspect, dataset1,
									correspondingResource1, this.getInputPrimaryModelUnion(dataset1));
							if (values1.isPresent()) {
								for (Resource correspondingResource2 : correspondingResources) {
									if (correspondingResource1.getURI()
											.compareTo(correspondingResource2.getURI()) >= 0) {
										// avoid doing work twice, but enable comparing representations of one resource
										// in different datasets
										for (Resource dataset2 : this.getInputDatasets()) {
											if (!correspondingResource1.equals(correspondingResource2)
													|| !dataset1.equals(dataset2)) {
												// avoid comparing the representation of one resource in one dataset
												// with itself
												Optional<Map<String, Set<RDFNode>>> values2 = Aspect.getResource(aspect,
														dataset1, correspondingResource1,
														this.getInputPrimaryModelUnion(dataset1));
												if (values2.isPresent()) {
													for (String variable : this.variables) {
														this.compareVariableValues(variable, dataset1,
																correspondingResource1,
																values1.get().getOrDefault(variable,
																		Collections.emptySet()),
																dataset2, correspondingResource2,
																values2.get().getOrDefault(variable,
																		Collections.emptySet()));
													}
												}
											}
										}
									}
								}
							}
						}
					}
				});
	}

	/**
	 * Compares the values of one variable from two corresponding resources in two
	 * datasets and stores encountered deviations and issues in both according
	 * outputMetaModels (derived by {@link #getOutputMetaModel(Resource)}). Either
	 * the corresponding resources or the datasets might be equal, but not both at
	 * once.
	 * 
	 * @param variable               Name of the compared variable
	 * @param dataset1               IRI of the first dataset
	 * @param correspondingResource1 IRI of the first resource
	 * @param values1                Variable values of the first resource
	 * @param dataset2               IRI of the second dataset
	 * @param correspondingResource2 IRI of the second resource
	 * @param values2                Variable values of the second resource
	 */
	public void compareVariableValues(String variable, Resource dataset1, Resource correspondingResource1,
			Set<RDFNode> values1, Resource dataset2, Resource correspondingResource2, Set<RDFNode> values2) {

		// remove invalid or wrong values and pairs of equivalent values
		Iterator<RDFNode> valuesIterator1, valuesIterator2;
		valuesIterator1 = values1.iterator();
		value1loop: while (valuesIterator1.hasNext()) {
			RDFNode value1 = valuesIterator1.next();
			if (this.isValidValue(value1)) {
				if (!Metadata.isWrongValue(correspondingResource1, variable, value1, aspect,
						this.getInputMetaModelUnion(dataset1))) {
					valuesIterator2 = values2.iterator();
					while (valuesIterator2.hasNext()) {
						RDFNode value2 = valuesIterator2.next();
						if (this.isValidValue(value2)) {
							if (!Metadata.isWrongValue(correspondingResource2, variable, value2, aspect,
									this.getInputMetaModelUnion(dataset2))) {
								if (this.equivalentValues(value1, value2)) {
									// remove pair of equivalent values
									valuesIterator1.remove();
									valuesIterator2.remove();
									continue value1loop;
								}
							} else {
								// remove wrong value to avoid further processing
								valuesIterator2.remove();
							}
						} else {
							// report invalid value
							Metadata.addIssue(correspondingResource2, variable, value2, aspect, "Invalid Value",
									this.invalidValueComment(), this.getOutputMetaModel(dataset2));
							// remove invalid value to avoid further processing
							valuesIterator2.remove();
						}
					}
				} else {
					// remove wrong value to avoid further processing
					valuesIterator1.remove();
				}
			} else {
				// report invalid value
				Metadata.addIssue(correspondingResource1, variable, value1, aspect, "Invalid Value",
						this.invalidValueComment(), this.getOutputMetaModel(dataset1));
				// remove invalid value to avoid further processing
				valuesIterator1.remove();
			}
		}

		if (values1.isEmpty()) {
			// report missing values
			for (RDFNode value2 : values2) {
				Metadata.addValuesOmission(correspondingResource1.asResource(), variable, dataset1,
						correspondingResource2.asResource(), value2, this.aspect, this.getOutputMetaModel(dataset1));
			}
		} else if (values2.isEmpty()) {
			// report missing values
			for (RDFNode value1 : values1) {
				Metadata.addValuesOmission(correspondingResource2.asResource(), variable, dataset2,
						correspondingResource1.asResource(), value1, this.aspect, this.getOutputMetaModel(dataset2));
			}
		} else {
			// report pairs of deviating values
			for (RDFNode value1 : values1) {
				for (RDFNode value2 : values2) {
					Metadata.addDeviation(correspondingResource1.asResource(), variable, value1, dataset2,
							correspondingResource2.asResource(), value2, this.aspect,
							this.getOutputMetaModel(dataset1));
					Metadata.addDeviation(correspondingResource2.asResource(), variable, value2, dataset1,
							correspondingResource1.asResource(), value1, this.aspect,
							this.getOutputMetaModel(dataset2));
				}
			}
		}
	}

	/**
	 * Checks if a value is valid.
	 * 
	 * @param variable the variable the value belongs to
	 * @param dataset  the dataset the value belongs to
	 * @param resource the resource the value belongs to
	 * @param value    the value to check
	 * @return {@code true}, if the value is valid, otherwise {@code false}
	 */
	public abstract boolean isValidValue(RDFNode value);

	public abstract String invalidValueComment();

	/**
	 * Checks if two values are equivalent.
	 * 
	 * @param variable the variable the value belongs to
	 * @param dataset  the dataset the value belongs to
	 * @param resource the resource the value belongs to
	 * @param value    the value to check
	 * @return {@code true}, if the value is valid, otherwise {@code false}
	 */
	public abstract boolean equivalentValues(RDFNode value1, RDFNode value2);
}
