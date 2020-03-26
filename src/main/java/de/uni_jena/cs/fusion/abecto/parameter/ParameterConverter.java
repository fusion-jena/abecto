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

import java.io.IOException;
import java.io.StringWriter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

@Converter
public class ParameterConverter implements AttributeConverter<ParameterModel, String> {
	
	@Autowired
	ObjectMapper objectMapper;
	
	protected final static char SEPARATOR = ':';

	@Override
	public String convertToDatabaseColumn(ParameterModel parametersObject) {
		StringWriter writer = new StringWriter();
		// append parameter object class name
		writer.append(parametersObject.getClass().getName());
		// append separator
		writer.append(SEPARATOR);
		// append parameter JSON
		try {
			objectMapper.writeValue(writer, parametersObject);
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize parameters to JSON.", e);
		}
		return writer.toString();
	}

	@Override
	public ParameterModel convertToEntityAttribute(String dbData) {
		// split attribute into class name and JSON
		int separatorIndex = dbData.indexOf(SEPARATOR);
		String parameterObjectClassName = dbData.substring(0, separatorIndex);
		String parameterJSON = dbData.substring(separatorIndex + 1);
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ParameterModel> parameterObejctClass = (Class<? extends ParameterModel>) Class
					.forName(parameterObjectClassName);
			return objectMapper.readValue(parameterJSON, parameterObejctClass);
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Failed to deserialize JSON to parameters.", e);
		}
	}
}
