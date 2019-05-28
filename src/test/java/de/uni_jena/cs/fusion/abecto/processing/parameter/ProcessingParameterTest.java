package de.uni_jena.cs.fusion.abecto.processing.parameter;

import static org.junit.Assert.*;

import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.junit.Test;

public class ProcessingParameterTest {

	@Test
	public void test() {
		ProcessingParameter processingParameter = new ProcessingParameter();
		assertTrue(processingParameter.getAll().isEmpty());
		processingParameter.put("key", "value");
		assertEquals("value", processingParameter.get("key", new TypeLiteral<String>() {}));
		Map<String, Object> map = processingParameter.getAll();
		assertTrue(map.containsKey("key"));
		assertEquals("value", (String) map.get("key"));
	}
}
