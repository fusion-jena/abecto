package de.uni_jena.cs.fusion.abecto.processing.parameter;

import org.junit.jupiter.api.Assertions;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

public class ProcessingParameterConverterTest {
	
	private String parameterClass = ProcessingParameterConverterTestParameterClass.class.getName();
	private String parameterJson = "{\"keyA\":\"valueA\",\"keyB\":\"valueB\"}";
	private ParameterModel parameterObject = new ProcessingParameterConverterTestParameterClass();

	@Test
	public void testConvertToDatabaseColumn() throws JSONException {
		String parameterSerialization = new ProcessingParameterConverter()
				.convertToDatabaseColumn(this.parameterObject);
		Assertions.assertTrue(parameterSerialization.startsWith(this.parameterClass));
		JSONAssert.assertEquals(parameterJson, parameterSerialization
				.substring(parameterSerialization.indexOf(ProcessingParameterConverter.SEPARATOR) + 1), false);
	}

	@Test
	public void testConvertToEntityAttribute() {
		ProcessingParameterConverterTestParameterClass actualParameters = (ProcessingParameterConverterTestParameterClass) new ProcessingParameterConverter()
				.convertToEntityAttribute(
						this.parameterClass + ProcessingParameterConverter.SEPARATOR + this.parameterJson);
		Assertions.assertEquals("valueA", actualParameters.keyA);
		Assertions.assertEquals("valueB", actualParameters.keyB);
	}
}