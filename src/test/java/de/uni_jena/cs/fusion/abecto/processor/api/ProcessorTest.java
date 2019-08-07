package de.uni_jena.cs.fusion.abecto.processor.api;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProcessorTest {

	@Test
	public void getParameterModel() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<? extends ProcessorParameters> modelClass;
		
		modelClass = Processor.getParameterClass(DummySuperProcessor.class);
		Assertions.assertEquals(DummyParameters.class, modelClass);
		
		modelClass = Processor.getParameterClass(DummySubProcessor.class);
		Assertions.assertEquals(DummyParameters.class, modelClass);
	}

	@Test
	public void getDefaultParameters() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		ProcessorParameters parameters;
		
		parameters = Processor.getDefaultParameters(DummySuperProcessor.class);
		Assertions.assertTrue(parameters instanceof DummyParameters);
		Assertions.assertEquals("default", ((DummyParameters) parameters).test);
		
		parameters = Processor.getDefaultParameters(DummySubProcessor.class);
		Assertions.assertTrue(parameters instanceof DummyParameters);
		Assertions.assertEquals("default", ((DummyParameters) parameters).test);
	}

	public static class DummyParameters implements ProcessorParameters {
		String test = "default";
	}

	public static class DummySuperProcessor extends AbstractProcessor<DummyParameters> {

		@Override
		public Map<UUID, Collection<Model>> getDataModels() {
			return null;
		}

		@Override
		public Collection<Model> getMetaModel() {
			return null;
		}

		@Override
		protected Model computeResultModel() throws Exception {
			return null;
		}

		@Override
		protected void prepare() throws Exception {
		}

	}

	public static class DummySubProcessor extends DummySuperProcessor {

	}

}
