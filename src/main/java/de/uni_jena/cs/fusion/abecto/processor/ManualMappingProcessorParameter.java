package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

@JsonSerialize
public class ManualMappingProcessorParameter implements ParameterModel {
	public Optional<Collection<Collection<String>>> mappings = Optional.empty();
	public Optional<Collection<Collection<String>>> suppressed_mappings = Optional.empty();
}
