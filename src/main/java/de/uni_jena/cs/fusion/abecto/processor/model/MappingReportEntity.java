package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.SortedMap;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.uni_jena.cs.fusion.abecto.sparq.LiteralSerializer;
import de.uni_jena.cs.fusion.abecto.sparq.ResourceSerializer;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class MappingReportEntity {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:MappingReportEntity")
	public Resource id;
	@SparqlPattern(subject = "id", predicate = "abecto:id1")
	public Optional<String> first;
	@SparqlPattern(subject = "id", predicate = "abecto:id2")
	public Optional<String> second;
	@SparqlPattern(subject = "id", predicate = "abecto:data1")
	public Optional<String> firstData;
	@SparqlPattern(subject = "id", predicate = "abecto:data2")
	public Optional<String> secondData;
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase1")
	public String firstKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase2")
	public String secondKnowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:category1")
	public Optional<String> firstCategory;
	@SparqlPattern(subject = "id", predicate = "abecto:category2")
	public Optional<String> secondCategory;

	public static MappingReportEntity ALL = new MappingReportEntity();

	private final static ObjectMapper JSON = new ObjectMapper()
			.registerModule(new SimpleModule("ResourceSerializer", new Version(1, 0, 0, null, null, null))
					.addSerializer(Resource.class, new ResourceSerializer())
					.addSerializer(Literal.class, new LiteralSerializer()));

	public MappingReportEntity() {
	}

	public MappingReportEntity(String first, String second, SortedMap<String, RDFNode> firstData,
			SortedMap<String, RDFNode> secondData, String firstKnowledgeBase, String secondKnowledgeBase,
			String category) throws JsonProcessingException {
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

	private static String formatData(SortedMap<String, RDFNode> data) throws JsonProcessingException {
		if (data == null) {
			return null;
		}
		return JSON.writeValueAsString(data);
	}

}
