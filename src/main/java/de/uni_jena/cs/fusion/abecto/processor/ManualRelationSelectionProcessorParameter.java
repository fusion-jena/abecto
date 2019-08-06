package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Optional;

public class ManualRelationSelectionProcessorParameter {
	Optional<Map<String, String>> relations = Optional.empty();
	Optional<Map<String, String>> suppressed_relations = Optional.empty();
}
