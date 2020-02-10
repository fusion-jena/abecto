package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class Issue {
	@SparqlPattern(predicate = "rdf:type", object = "abecto:Issue")
	public Resource id;
	@SparqlPattern(subject = "id", predicate = "abecto:affectedKnowledgeBase")
	public final UUID knowledgeBase;
	@SparqlPattern(subject = "id", predicate = "abecto:affectedEntity")
	public final Resource entity;
	@SparqlPattern(subject = "id", predicate = "abecto:issueType")
	public final String type;
	@SparqlPattern(subject = "id", predicate = "abecto:issueMessage")
	public final String message;

	public Issue() {
		this.id = null;
		this.knowledgeBase = null;
		this.entity = null;
		this.type = null;
		this.message = null;
	}

	public Issue(@Member("id") Resource id, @Member("knowledgeBase") UUID knowledgeBase,
			@Member("entity") Resource entity, @Member("type") String type, @Member("message") String message) {
		this.id = id;
		this.knowledgeBase = knowledgeBase;
		this.entity = entity;
		this.type = type;
		this.message = message;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Issue && this.knowledgeBase == ((Issue) other).knowledgeBase
				&& this.entity == ((Issue) other).entity && this.type == ((Issue) other).type
				&& this.message == ((Issue) other).message;
	}

	@Override
	public int hashCode() {
		return this.entity.hashCode() + this.type.hashCode() + this.knowledgeBase.hashCode() + this.message.hashCode();
	}

}
