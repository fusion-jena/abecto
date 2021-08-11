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

		for (Field field : processor.getClass().getFields()) {
			if (field.isAnnotationPresent(Parameter.class)) {
				List<?> values = parameters.get(field.getName());
				if (Collection.class.isAssignableFrom(field.getType())) {
					if (values != null) {
						Collection parameter = Collection.class.cast(field.get(processor));
						// parameter collection not initialized
						if (parameter == null) {
							parameter = new ArrayList();
							field.set(processor, parameter);
						}
						// clear default values
						parameter.clear();
						// add values
						Collection.class.cast(field.get(processor)).addAll(values);
					}
				} else {
					if (values != null && values.size() > 1) {
						throw new ToManyElementsException(
								String.format("More than one value for parameter \"%s\"", field.getName()));
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
										String.format("Missing value for required parameter \"%s\"", field.getName()));
							}
						} else if (values.size() == 1) {
							field.set(processor, values.get(0));
						}
					}
				}
			}
		}
	}
}
