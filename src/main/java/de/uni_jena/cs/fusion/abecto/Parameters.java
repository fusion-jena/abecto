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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;

public class Parameters {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setParameters(Processor processor, Map<String, List<?>> parameters)
			throws ReflectiveOperationException, NoSuchElementException, ToManyElementsException,
			IllegalArgumentException {

		ArrayList<String> unusedParameters = new ArrayList<>(parameters.keySet());
		for (Field field : processor.getClass().getFields()) {
			if (field.isAnnotationPresent(Parameter.class)) {
				List<?> values = parameters.get(field.getName());
				unusedParameters.remove(field.getName());
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection parameter = Collection.class.cast(field.get(processor));
					// parameter collection not initialized
					if (parameter == null) {
						parameter = new ArrayList();
						field.set(processor, parameter);
					}
					if (values != null) {
						// clear default values
						parameter.clear();
						// add values
						Collection.class.cast(field.get(processor)).addAll(values);
					}
				} else {
					if (values != null && values.size() > 1) {
						throw new ToManyElementsException(
								String.format("More than one value for parameter \"%s\" of %s.", field.getName(), processor.getClass().getSimpleName()));
					}
					if (Optional.class.isAssignableFrom(field.getType())) {
						if (values == null || values.isEmpty()) {
							field.set(processor, Optional.empty());
						} else {
							Object value = values.get(0);
							field.set(processor, Optional.of(value));
						}
					} else {
						if ((values == null || values.isEmpty())) {
							if (field.get(processor) == null) {
								throw new NoSuchElementException(
										String.format("Missing value for required parameter \"%s\" of %s.", field.getName(), processor.getClass().getSimpleName()));
							}
						} else {
							field.set(processor, values.get(0));
						}
					}
				}
			}
		}
		if (!unusedParameters.isEmpty()) {
			throw new IllegalArgumentException(String.format("Unexpected parameters for %s: %s",
					processor.getClass().getSimpleName(), String.join(", ", unusedParameters)));
		}
	}
}
