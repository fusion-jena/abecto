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

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

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