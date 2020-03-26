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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.parameter_model.EmptyParameters;
import de.uni_jena.cs.fusion.abecto.processor.AbstractProcessor;
import de.uni_jena.cs.fusion.abecto.processor.AbstractRefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.RefinementProcessor;

public class AbstractRefinementProcessorTest {

	@Test
	public void prepare() {
		// fails on already failed input processors
		RefinementProcessor<?> refinement = new SomeRefinementProcessor();
		refinement.addInputProcessor(new SomeFailedProcessor());
		Assertions.assertThrows(ExecutionException.class, refinement::call);
	}

	private static class SomeFailedProcessor extends AbstractProcessor<EmptyParameters> {

		@Override
		public Map<UUID, Collection<Model>> getDataModels() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Model> getMetaModels() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void computeResultModel() throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void prepare() throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isFailed() {
			return true;
		}

		@Override
		public Status getStatus() {
			return Processor.Status.FAILED;
		}

		@Override
		public UUID getOntology() {
			throw new NoSuchElementException();
		}
	}

	private static class SomeRefinementProcessor extends AbstractRefinementProcessor<EmptyParameters> {

		@Override
		public Map<UUID, Collection<Model>> getDataModels() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Model> getMetaModels() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void computeResultModel() throws Exception {
			throw new UnsupportedOperationException();
		}

	}

}
