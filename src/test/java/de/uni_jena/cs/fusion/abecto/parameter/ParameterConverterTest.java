package de.uni_jena.cs.fusion.abecto.parameter;

import org.junit.jupiter.api.Assertions;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import de.uni_jena.cs.fusion.abecto.parameter.ParameterConverter;
import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

public class ParameterConverterTest {
	
	private String parameterClass = ParameterConverterTestParameterModel.class.getName();
	private String parameterJson = "{\"keyA\":\"valueA\",\"keyB\":\"valueB\"}";
	private ParameterModel parameterObject = new ParameterConverterTestParameterModel();

	@Test
	public void testConvertToDatabaseColumn() throws JSONException {
		String parameterSerialization = new ParameterConverter()
				.convertToDatabaseColumn(this.parameterObject);
		Assertions.assertTrue(parameterSerialization.startsWith(this.parameterClass));
		JSONAssert.assertEquals(parameterJson, parameterSerialization
				.substring(parameterSerialization.indexOf(ParameterConverter.SEPARATOR) + 1), false);
	}

	@Test
	public void testConvertToEntityAttribute() {
		ParameterConverterTestParameterModel actualParameters = (ParameterConverterTestParameterModel) new ParameterConverter()
				.convertToEntityAttribute(
						this.parameterClass + ParameterConverter.SEPARATOR + this.parameterJson);
		Assertions.assertEquals("valueA", actualParameters.keyA);
		Assertions.assertEquals("valueB", actualParameters.keyB);
	}
}