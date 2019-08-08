package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

public class ManualMappingProcessorParameter implements ParameterModel {
	public Optional<Collection<Collection<String>>> mappings = Optional.empty();
	public Optional<Collection<Collection<String>>> suppressed_mappings = Optional.empty();
}
