package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

@JsonSerialize
public class ManualRelationSelectionProcessorParameter implements ParameterModel {
	public Optional<Map<String, String>> relations = Optional.empty();
	public Optional<Map<String, String>> suppressed_relations = Optional.empty();
}
