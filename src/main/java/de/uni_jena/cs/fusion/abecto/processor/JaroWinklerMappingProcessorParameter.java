package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Optional;

public class JaroWinklerMappingProcessorParameter {
	double threshold;
	boolean case_sensitive;
	Optional<String> property = Optional.empty();
}
