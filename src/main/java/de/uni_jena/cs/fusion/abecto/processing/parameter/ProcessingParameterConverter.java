package de.uni_jena.cs.fusion.abecto.processing.parameter;

import java.io.IOException;
import java.io.StringWriter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_jena.cs.fusion.abecto.processor.api.ProcessorParameters;

@Converter
public class ProcessingParameterConverter implements AttributeConverter<ProcessorParameters, String> {

	private final static ObjectMapper JSON = new ObjectMapper();
	protected final static char SEPARATOR = ':';

	@Override
	public String convertToDatabaseColumn(ProcessorParameters parametersObject) {
		StringWriter writer = new StringWriter();
		// append parameter object class name
		writer.append(parametersObject.getClass().getName());
		// append separator
		writer.append(SEPARATOR);
		// append parameter JSON
		try {
			JSON.writeValue(writer, parametersObject);
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize parameters to JSON.", e);
		}
		return writer.toString();
	}

	@Override
	public ProcessorParameters convertToEntityAttribute(String dbData) {
		// split attribute into class name and JSON
		int separatorIndex = dbData.indexOf(SEPARATOR);
		String parameterObjectClassName = dbData.substring(0, separatorIndex);
		String parameterJSON = dbData.substring(separatorIndex + 1);
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ProcessorParameters> parameterObejctClass = (Class<? extends ProcessorParameters>) Class
					.forName(parameterObjectClassName);
			return JSON.readValue(parameterJSON, parameterObejctClass);
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Failed to deserialize JSON to parameters.", e);
		}
	}
}
