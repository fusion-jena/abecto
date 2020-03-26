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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;

public class ProcessorTest {

	@Test
	public void getParameterModel() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<? extends ParameterModel> modelClass;

		modelClass = Processor.getParameterClass(DummySuperProcessor.class);
		Assertions.assertEquals(DummyParameters.class, modelClass);

		modelClass = Processor.getParameterClass(DummySubProcessor.class);
		Assertions.assertEquals(DummyParameters.class, modelClass);
	}

	@Test
	public void getDefaultParameters() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		ParameterModel parameters;

		parameters = Processor.getDefaultParameters(DummySuperProcessor.class);
		Assertions.assertTrue(parameters instanceof DummyParameters);
		Assertions.assertEquals("default", ((DummyParameters) parameters).test);

		parameters = Processor.getDefaultParameters(DummySubProcessor.class);
		Assertions.assertTrue(parameters instanceof DummyParameters);
		Assertions.assertEquals("default", ((DummyParameters) parameters).test);
	}

	public static class DummyParameters implements ParameterModel {
		String test = "default";
	}

	public static class DummySuperProcessor extends AbstractProcessor<DummyParameters> {

		@Override
		public Map<UUID, Collection<Model>> getDataModels() {
			return null;
		}

		@Override
		public Collection<Model> getMetaModels() {
			return null;
		}

		@Override
		protected void computeResultModel() throws Exception {
		}

		@Override
		protected void prepare() throws Exception {
		}

		@Override
		public UUID getOntology() {
			throw new NoSuchElementException();
		}

	}

	public static class DummySubProcessor extends DummySuperProcessor {

	}

}
