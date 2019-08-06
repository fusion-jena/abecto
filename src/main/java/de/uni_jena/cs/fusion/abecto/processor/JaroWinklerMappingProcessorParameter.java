package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.api.ProcessorParameters;

public class JaroWinklerMappingProcessorParameter implements ProcessorParameters {
	double threshold;
	boolean case_sensitive;
	Optional<String> property = Optional.empty();
}
