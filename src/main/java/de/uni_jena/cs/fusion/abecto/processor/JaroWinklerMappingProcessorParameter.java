package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.api.ParameterModel;

public class JaroWinklerMappingProcessorParameter implements ParameterModel {
	public double threshold;
	public boolean case_sensitive;
	public Optional<String> property = Optional.empty();
}
