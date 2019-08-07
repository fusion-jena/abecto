package de.uni_jena.cs.fusion.abecto.processor;

import java.util.Map;
import java.util.Optional;

import de.uni_jena.cs.fusion.abecto.processor.api.ProcessorParameters;

public class ManualRelationSelectionProcessorParameter implements ProcessorParameters {
	public Optional<Map<String, String>> relations = Optional.empty();
	public Optional<Map<String, String>> suppressed_relations = Optional.empty();
}
