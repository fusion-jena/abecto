package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Collection;
import java.util.Optional;

public class ManualMappingProcessorParameter {
	Optional<Collection<Collection<String>>> mappings = Optional.empty();
	Optional<Collection<Collection<String>>> suppressed_mappings = Optional.empty();
}
