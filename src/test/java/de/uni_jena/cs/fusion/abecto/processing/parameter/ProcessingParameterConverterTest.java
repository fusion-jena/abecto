package de.uni_jena.cs.fusion.abecto.processing.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class ProcessingParameterConverterTest {

	private String json = "{\"keyA\":\"valueA\",\"keyB\":\"valueB\"}";
	private Map<String, Object> map = Map.of("keyA", "valueA", "keyB", "valueB");

	@Test
	public void testConvertToDatabaseColumn() throws JSONException {
		JSONAssert.assertEquals(json, new ProcessingParameterConverter().convertToDatabaseColumn(map), false);
	}

	@Test
	public void testConvertToEntityAttribute() {
		Map<String, Object> actualMap = new ProcessingParameterConverter().convertToEntityAttribute(json);
		assertEquals(map, actualMap);
		assertEquals(map.size(), actualMap.size());
		for (Entry<String, Object> entry : map.entrySet()) {
			assertEquals(entry.getValue(), actualMap.get(entry.getKey()),
					"Different values for key \"" + entry.getValue() + "\"");
		}
	}

}
