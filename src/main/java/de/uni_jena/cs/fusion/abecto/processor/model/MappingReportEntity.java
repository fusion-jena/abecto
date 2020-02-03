package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
	public String first;
	@SparqlPattern(subject = "id", predicate = "abecto:secondId")
	public String second;
	@SparqlPattern(subject = "id", predicate = "abecto:firstData")
	public Map<String, RDFNode> firstData;
	@SparqlPattern(subject = "id", predicate = "abecto:secondData")
	public Map<String, RDFNode> secondData;
	@SparqlPattern(subject = "id", predicate = "abecto:firstSourceKnowledgeBase")
	public Optional<String> firstKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:secondSourceKnowledgeBase")
	public Optional<String> secondKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:firstCategorisedAs")
	public Optional<String> firstCategory;
	@SparqlPattern(subject = "id", predicate = "abecto:secondCategorisedAs")
	public Optional<String> secondCategory;

	public MappingReportEntity(String first, String second, Map<String, RDFNode> firstData,
			Map<String, RDFNode> secondData, String firstKnowledgeBase, String secondKnowledgeBase, String category) {
		this.first = first;
		this.second = second;
		this.firstData = firstData;
		this.secondData = secondData;
		this.firstKnowledgeBase = Optional.of(firstKnowledgeBase);
		this.secondKnowledgeBase = Optional.of(secondKnowledgeBase);
		this.firstCategory = Optional.of(category);
		this.secondCategory = Optional.of(category);
	}

	public MappingReportEntity(PositiveMapping mapping, Map<String, RDFNode> firstData,
			Map<String, RDFNode> secondData) {
		this.first = mapping.first.getURI();
		this.second = mapping.second.getURI();
		this.firstData = firstData;
		this.secondData = secondData;
		this.firstKnowledgeBase = mapping.firstKnowledgeBase.map(UUID::toString);
		this.secondKnowledgeBase = mapping.secondKnowledgeBase.map(UUID::toString);
		this.firstCategory = mapping.firstCategory;
		this.secondCategory = mapping.secondCategory;
	}
}
