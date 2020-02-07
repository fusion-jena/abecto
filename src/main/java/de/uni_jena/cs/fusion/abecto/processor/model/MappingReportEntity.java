package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class MappingReportEntity {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:MappingReportEntity")
	public Resource id;
	@SparqlPattern(subject = "id", predicate = "abecto:firstId")
	public Optional<String> first;
	@SparqlPattern(subject = "id", predicate = "abecto:secondId")
	public Optional<String> second;
	@SparqlPattern(subject = "id", predicate = "abecto:firstData")
	public Optional<String> firstData;
	@SparqlPattern(subject = "id", predicate = "abecto:secondData")
	public Optional<String> secondData;
	@SparqlPattern(subject = "id", predicate = "abecto:firstSourceKnowledgeBase")
	public String firstKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:secondSourceKnowledgeBase")
	public String secondKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:firstCategorisedAs")
	public Optional<String> firstCategory;
	@SparqlPattern(subject = "id", predicate = "abecto:secondCategorisedAs")
	public Optional<String> secondCategory;

	public static MappingReportEntity ALL = new MappingReportEntity();

	public MappingReportEntity() {
	}

	public MappingReportEntity(String first, String second, SortedMap<String, RDFNode> firstData,
			SortedMap<String, RDFNode> secondData, String firstKnowledgeBase, String secondKnowledgeBase,
			String category) {
		if (firstKnowledgeBase.compareTo(secondKnowledgeBase) > 0) {
			this.first = Optional.ofNullable(first);
			this.second = Optional.ofNullable(second);
			this.firstData = Optional.ofNullable(formatData(firstData));
			this.secondData = Optional.ofNullable(formatData(secondData));
			this.firstKnowledgeBase = firstKnowledgeBase;
			this.secondKnowledgeBase = secondKnowledgeBase;
		} else {
			this.first = Optional.ofNullable(second);
			this.second = Optional.ofNullable(first);
			this.firstData = Optional.ofNullable(formatData(secondData));
			this.secondData = Optional.ofNullable(formatData(firstData));
			this.firstKnowledgeBase = secondKnowledgeBase;
			this.secondKnowledgeBase = firstKnowledgeBase;
		}
		this.firstCategory = Optional.ofNullable(category);
		this.secondCategory = Optional.ofNullable(category);
	}

	public MappingReportEntity(Mapping mapping, SortedMap<String, RDFNode> firstData,
			SortedMap<String, RDFNode> secondData) {
		if (!mapping.entitiesMap) {
			throw new IllegalArgumentException("Given mapping is negative.");
		}
		if (mapping.firstKnowledgeBase.get().compareTo(mapping.secondKnowledgeBase.get()) > 0) {
			this.first = Optional.ofNullable(mapping.first.getURI());
			this.second = Optional.ofNullable(mapping.second.getURI());
			this.firstData = Optional.ofNullable(formatData(firstData));
			this.secondData = Optional.ofNullable(formatData(secondData));
			this.firstKnowledgeBase = mapping.firstKnowledgeBase.map(UUID::toString).get();
			this.secondKnowledgeBase = mapping.secondKnowledgeBase.map(UUID::toString).get();
			this.firstCategory = mapping.firstCategory;
			this.secondCategory = mapping.secondCategory;
		} else {
			this.first = Optional.ofNullable(mapping.second.getURI());
			this.second = Optional.ofNullable(mapping.first.getURI());
			this.firstData = Optional.ofNullable(formatData(secondData));
			this.secondData = Optional.ofNullable(formatData(firstData));
			this.firstKnowledgeBase = mapping.secondKnowledgeBase.map(UUID::toString).get();
			this.secondKnowledgeBase = mapping.firstKnowledgeBase.map(UUID::toString).get();
			this.firstCategory = mapping.secondCategory;
			this.secondCategory = mapping.firstCategory;
		}
	}

	private static String formatData(SortedMap<String, RDFNode> data) {
		if (data == null) {
			return null;
		}
		int maxLength = data.keySet().stream().map(String::length).max(Integer::compareTo).orElse(0);
		return data.entrySet().stream().map((entry) -> {
			return String.format("%1$" + maxLength + "s: %s", entry.getKey(), entry.getValue().toString());
		}).collect(Collectors.joining("\n"));
	}

}
