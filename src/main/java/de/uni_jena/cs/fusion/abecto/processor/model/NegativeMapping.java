package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class NegativeMapping implements Mapping {
	public final Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:notMappableTo")
	public final Resource second;
	@SparqlPattern(subject = "first", predicate = "abecto:sourceKnowledgeBase")
	public Optional<UUID> firstKnowledgeBase;
	@SparqlPattern(subject = "second", predicate = "abecto:sourceKnowledgeBase")
	public Optional<UUID> secondKnowledgeBase;
	@SparqlPattern(subject = "first", predicate = "abecto:categorisedAs")
	public Optional<String> firstCategory;
	@SparqlPattern(subject = "second", predicate = "abecto:categorisedAs")
	public Optional<String> secondCategory;

	public final static NegativeMapping prototype = new NegativeMapping(null, null, null, null, null, null);

	private final static int HASH_OFFSET = 1;

	public NegativeMapping(@Member("first") Resource first, @Member("second") Resource second,
			@Member("firstKnowledgeBase") Optional<UUID> firstKnowledgeBase,
			@Member("secondKnowledgeBase") Optional<UUID> secondKnowledgeBase,
			@Member("firstCategory") Optional<String> firstCategory,
			@Member("secondCategory") Optional<String> secondCategory) {
		this.first = first;
		this.second = second;
		this.firstKnowledgeBase = firstKnowledgeBase;
		this.secondKnowledgeBase = secondKnowledgeBase;
		this.firstCategory = firstCategory;
		this.secondCategory = secondCategory;
	}

	public Mapping inverse() {
		return new PositiveMapping(this.first, this.second, this.firstKnowledgeBase, this.secondKnowledgeBase,
				this.firstCategory, this.secondCategory);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NegativeMapping))
			return false;
		NegativeMapping other = (NegativeMapping) o;
		return this.first.equals(other.first) && this.second.equals(other.second)
				|| this.first.equals(other.second) && this.second.equals(other.first);
	}

	@Override
	public int hashCode() {
		return first.getURI().hashCode() + second.getURI().hashCode() + HASH_OFFSET;
	}

	@Override
	public void setKnowledgeBases(UUID firstKnowledgeBase, UUID secondKnowledgeBase) {
		this.firstKnowledgeBase = Optional.ofNullable(firstKnowledgeBase);
		this.secondKnowledgeBase = Optional.ofNullable(secondKnowledgeBase);
	}

	@Override
	public NegativeMapping setCategories(String categorie) {
		this.firstCategory = Optional.ofNullable(categorie);
		this.secondCategory = Optional.ofNullable(categorie);
		return this;
	}

}