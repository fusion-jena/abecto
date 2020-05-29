package de.uni_jena.cs.fusion.abecto.metaentity;

import java.util.Objects;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto#")
public class Omission {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Measurement")
	public final Resource id;
	/** The category of the missing resource. */
	@SparqlPattern(subject = "id", predicate = "abecto:categoryName")
	public final String categoryName;
	/** The ontology which is missing the resource. */
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase")
	public final UUID ontology;
	/** The resource which is missing. */
	@SparqlPattern(subject = "id", predicate = "abecto:resource1")
	public final Resource resource;
	/** The ontology which contains the missing resource. */
	@SparqlPattern(subject = "id", predicate = "abecto:knowledgeBase")
	public final UUID source;

	public Omission(@Member("id") Resource id, @Member("categoryName") String categoryName,
			@Member("ontology") UUID ontology, @Member("resource") Resource resource, @Member("source") UUID source) {
		this.id = id;
		this.categoryName = categoryName;
		this.ontology = ontology;
		this.resource = resource;
		this.source = source;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Omission)) {
			return false;
		}
		Omission o = (Omission) other;
		return Objects.equals(this.categoryName, o.categoryName) && Objects.equals(this.ontology, o.ontology)
				&& Objects.equals(this.resource, o.resource) && Objects.equals(this.source, o.source);
	}

	@Override
	public int hashCode() {
		return this.categoryName.hashCode() + this.ontology.hashCode() + this.resource.hashCode()
				+ this.source.hashCode();
	}
}
