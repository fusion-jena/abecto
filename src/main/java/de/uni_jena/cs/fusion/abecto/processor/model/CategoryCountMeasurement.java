package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class CategoryCountMeasurement {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:CategoryCountMeasure")
	public Resource id;
	/**
	 * The name of the category for which the count was measured.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:categoryName")
	public String categoryName;
	/**
	 * The variable of the categories whose count was measured. If empty, the count
	 * of category entities was measured.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:variableName")
	public Optional<String> variableName;
	/**
	 * The measured value.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:value")
	public Long value;
	/**
	 * The knowledge base on which the value was measured.
	 */
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase")
	public UUID knowledgeBase;

	public CategoryCountMeasurement() {
	}

	public CategoryCountMeasurement(String categoryName, Optional<String> variableName, Long value, UUID knowledgeBase) {
		this.categoryName = categoryName;
		this.variableName = variableName;
		this.value = value;
		this.knowledgeBase = knowledgeBase;
	}
}
