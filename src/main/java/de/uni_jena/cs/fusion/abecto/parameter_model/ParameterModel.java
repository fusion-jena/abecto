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
package de.uni_jena.cs.fusion.abecto.parameter_model;

import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.Processor;

/**
 * Provides an interface for parameter objects of {@link Processor}s.
 * <p>
 * {@link ParameterModel}s are linked to {@link Processor}s using the type
 * parameter of the {@link Processor} interface. Members of
 * {@link ParameterModel}s need to be public. Members for optional parameters
 * should be declared using {@link Optional}. Other parameters are interpreted
 * as mandatory. {@link ParameterModel}s will be serialized and deserialized
 * using {@link com.fasterxml.jackson.databind.ObjectMapper}. The use of
 * {@link com.fasterxml.jackson.annotation.JacksonAnnotation}s is permitted.
 */
public interface ParameterModel {

}
