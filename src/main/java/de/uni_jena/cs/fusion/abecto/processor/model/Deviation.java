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
	@SparqlPattern(subject = "id", predicate = "abecto:ontologyId1")
	public final UUID ontologyId1;
	@SparqlPattern(subject = "id", predicate = "abecto:ontologyId2")
	public final UUID ontologyId2;
	@SparqlPattern(subject = "id", predicate = "abecto:value1")
	public final String value1;
	@SparqlPattern(subject = "id", predicate = "abecto:value2")
	public final String value2;

	public Deviation() {
		this(null, null, null, null, null, null, null, null, null);
	}

	public Deviation(@Member("id") Resource id, @Member("categoryName") String categoryName,
			@Member("variableName") String variableName, @Member("resource1") Resource resource1,
			@Member("resource2") Resource resource2, @Member("ontologyId1") UUID ontologyId1,
			@Member("ontologyId2") UUID ontologyId2, @Member("value1") String value1,
			@Member("value2") String value2) {
		this.id = id;
		this.categoryName = categoryName;
		this.variableName = variableName;
		if (resource1 == null || resource2 != null && resource1.getURI().compareTo(resource2.getURI()) < 0) {
			this.resource1 = resource1;
			this.resource2 = resource2;
			this.ontologyId1 = ontologyId1;
			this.ontologyId2 = ontologyId2;
			this.value1 = value1;
			this.value2 = value2;
		} else {
			this.resource1 = resource2;
			this.resource2 = resource1;
			this.ontologyId1 = ontologyId2;
			this.ontologyId2 = ontologyId1;
			this.value1 = value2;
			this.value2 = value1;
		}
	}

	public Resource getResourceOf(UUID ontology) {
		if (this.ontologyId1 != null && ontology.equals(this.ontologyId1)) {
			return this.resource1;
		} else if (this.ontologyId2 != null && ontology.equals(this.ontologyId2)) {
			return this.resource2;
		} else {
			throw new IllegalArgumentException("Unknown ontology.");
		}
	}

	public String getValueOf(UUID ontology) {
		if (this.ontologyId1 != null && ontology.equals(this.ontologyId1)) {
			return this.value1;
		} else if (this.ontologyId2 != null && ontology.equals(this.ontologyId2)) {
			return this.value2;
		} else {
			throw new IllegalArgumentException("Unknown ontology.");
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
				&& Objects.equals(this.ontologyId1, o.ontologyId1)
				&& Objects.equals(this.ontologyId2, o.ontologyId2) && Objects.equals(this.value1, o.value1)
				&& Objects.equals(this.value2, o.value2);
	}

	@Override
	public int hashCode() {
		return this.categoryName.hashCode() + this.variableName.hashCode() + this.resource1.hashCode()
				+ this.resource2.hashCode() + this.ontologyId1.hashCode() + this.ontologyId2.hashCode()
				+ this.value1.hashCode() + this.value2.hashCode();
	}

}
