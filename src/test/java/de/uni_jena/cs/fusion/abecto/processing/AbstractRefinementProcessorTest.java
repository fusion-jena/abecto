package de.uni_jena.cs.fusion.abecto.processing;

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
		public Collection<Model> getMetaModel() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected Model computeResultModel() throws Exception {
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
		public UUID getKnowledgeBase() {
			throw new NoSuchElementException();
		}
	}

	private static class SomeRefinementProcessor extends AbstractRefinementProcessor<EmptyParameters> {

		@Override
		public Map<UUID, Collection<Model>> getDataModels() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Model> getMetaModel() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected Model computeResultModel() throws Exception {
			throw new UnsupportedOperationException();
		}

	}

}
