package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Objects;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Deviation {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Deviation")
	public Resource id;
	@SparqlPattern(subject = "id", predicate = "abecto:categoryName")
	public final String categoryName;
	@SparqlPattern(subject = "id", predicate = "abecto:variableName")
	public final String variableName;
	@SparqlPattern(subject = "id", predicate = "abecto:resource1")
	public final Resource resource1;
	@SparqlPattern(subject = "id", predicate = "abecto:resource2")
	public final Resource resource2;
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBaseId1")
	public final UUID knowledgeBaseId1;
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBaseId2")
	public final UUID knowledgeBaseId2;
	@SparqlPattern(subject = "id", predicate = "abecto:value1")
	public final String value1;
	@SparqlPattern(subject = "id", predicate = "abecto:value2")
	public final String value2;

	public Deviation(@Member("id") Resource id, @Member("categoryName") String categoryName,
			@Member("variableName") String variableName, @Member("resource1") Resource resource1,
			@Member("resource2") Resource resource2, @Member("knowledgeBaseId1") UUID knowledgeBaseId1,
			@Member("knowledgeBaseId2") UUID knowledgeBaseId2, @Member("value1") String value1,
			@Member("value2") String value2) {
		this.id = id;
		this.categoryName = categoryName;
		this.variableName = variableName;
		if (resource1 == null || resource2 != null && resource1.getURI().compareTo(resource2.getURI()) < 0) {
			this.resource1 = resource1;
			this.resource2 = resource2;
			this.knowledgeBaseId1 = knowledgeBaseId1;
			this.knowledgeBaseId2 = knowledgeBaseId2;
			this.value1 = value1;
			this.value2 = value2;
		} else {
			this.resource1 = resource2;
			this.resource2 = resource1;
			this.knowledgeBaseId1 = knowledgeBaseId2;
			this.knowledgeBaseId2 = knowledgeBaseId1;
			this.value1 = value2;
			this.value2 = value1;
		}
	}

	public Resource getResourceOf(UUID knowledgeBase) {
		if (this.knowledgeBaseId1 != null && knowledgeBase.equals(this.knowledgeBaseId1)) {
			return this.resource1;
		} else if (this.knowledgeBaseId2 != null && knowledgeBase.equals(this.knowledgeBaseId2)) {
			return this.resource2;
		} else {
			throw new IllegalArgumentException("Unknown knowledge base.");
		}
	}

	public String getValueOf(UUID knowledgeBase) {
		if (this.knowledgeBaseId1 != null && knowledgeBase.equals(this.knowledgeBaseId1)) {
			return this.value1;
		} else if (this.knowledgeBaseId2 != null && knowledgeBase.equals(this.knowledgeBaseId2)) {
			return this.value2;
		} else {
			throw new IllegalArgumentException("Unknown knowledge base.");
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Deviation)) {
			return false;
		}
		Deviation o = (Deviation) other;
		return Objects.equals(this.categoryName, o.categoryName) && Objects.equals(this.variableName, o.variableName)
				&& Objects.equals(this.resource1, o.resource1) && Objects.equals(this.resource2, o.resource2)
				&& Objects.equals(this.knowledgeBaseId1, o.knowledgeBaseId1)
				&& Objects.equals(this.knowledgeBaseId2, o.knowledgeBaseId2) && Objects.equals(this.value1, o.value1)
				&& Objects.equals(this.value2, o.value2);
	}

	@Override
	public int hashCode() {
		return this.categoryName.hashCode() + this.variableName.hashCode() + this.resource1.hashCode()
				+ this.resource2.hashCode() + this.knowledgeBaseId1.hashCode() + this.knowledgeBaseId2.hashCode()
				+ this.value1.hashCode() + this.value2.hashCode();
	}

}
