package de.uni_jena.cs.fusion.abecto.report.model;

import java.util.Optional;
import java.util.UUID;

import de.uni_jena.cs.fusion.abecto.processor.model.PositiveMapping;

public class MappingReportEntity {
	public String first;
	public String second;
	public Optional<String> firstKnowledgeBase;
	public Optional<String> secondKnowledgeBase;
	public Optional<String> firstCategory;
	public Optional<String> secondCategory;
	
	public MappingReportEntity(PositiveMapping mapping) {
		this.first =mapping.first.getURI();
		this.second =mapping.second.getURI();
		this.firstKnowledgeBase = mapping.firstKnowledgeBase.map(UUID::toString);
		this.secondKnowledgeBase = mapping.secondKnowledgeBase.map(UUID::toString);
		this.firstCategory = mapping.firstCategory;
		this.secondCategory = mapping.secondCategory;
	}
}
