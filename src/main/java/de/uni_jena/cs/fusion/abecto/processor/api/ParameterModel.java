package de.uni_jena.cs.fusion.abecto.processor.api;

import java.util.Optional;

/**
 * {@link ParameterModel} is an interface for parameter objects of
 * {@link Processor}s. {@link ParameterModel}s are linked to {@link Processor}s
 * using the type parameter of the {@link Processor} interface. Members of
 * {@link ParameterModel}s need to be public. Members for optional parameters
 * should be declared using {@link Optional}. Other parameters are interpreted
 * as mandatory. {@link ParameterModel}s will be serialized and deserialized
 * using {@link com.fasterxml.jackson.databind.ObjectMapper}. The use of
 * {@link com.fasterxml.jackson.annotation.JacksonAnnotation}s is permitted.
 */
public interface ParameterModel {

}
