package de.uni_jena.cs.fusion.abecto.parameter;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ParameterConverterTest {

	private String parameterClass = TestParameterModel.class.getName();
	private String parameterJson = "{\"keyA\":\"valueA\",\"keyB\":\"valueB\"}";
	private ParameterModel parameterObject = new TestParameterModel();

	@Autowired
	ObjectMapper objectMapper;
	ParameterConverter parameterConverter;
	
	@BeforeEach
	public void prepareParameterConverter() {
		parameterConverter = new ParameterConverter();
		// set objectMapper as it will not get autowired
		parameterConverter.objectMapper = objectMapper;
	}

	@Test
	public void testConvertToDatabaseColumn() throws JSONException {
		String parameterSerialization = parameterConverter.convertToDatabaseColumn(this.parameterObject);
		Assertions.assertTrue(parameterSerialization.startsWith(this.parameterClass));
		JSONAssert.assertEquals(parameterJson,
				parameterSerialization.substring(parameterSerialization.indexOf(ParameterConverter.SEPARATOR) + 1),
				false);
	}

	@Test
	public void testConvertToEntityAttribute() {
		TestParameterModel actualParameters = (TestParameterModel) parameterConverter
				.convertToEntityAttribute(this.parameterClass + ParameterConverter.SEPARATOR + this.parameterJson);
		Assertions.assertEquals("valueA", actualParameters.keyA);
		Assertions.assertEquals("valueB", actualParameters.keyB);
	}
	
	public static class TestParameterModel implements ParameterModel {
		public String keyA = "valueA";
		public String keyB = "valueB";
		
		public TestParameterModel() {}
	}
}