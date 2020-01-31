package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class PositiveMapping implements Mapping {
	public final Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:mappedTo")
	public final Resource second;
	@SparqlPattern(subject = "first", predicate = "abecto:sourceKnowledgeBase")
	public Optional<UUID> firstKnowledgeBase;
	@SparqlPattern(subject = "second", predicate = "abecto:sourceKnowledgeBase")
	public Optional<UUID> secondKnowledgeBase;
	@SparqlPattern(subject = "first", predicate = "abecto:categorisedAs")
	public Optional<String> firstCategory;
	@SparqlPattern(subject = "second", predicate = "abecto:categorisedAs")
	public Optional<String> secondCategory;

	public final static PositiveMapping prototype = new PositiveMapping(null, null, null, null, null, null);

	private final static int HASH_OFFSET = 0;

	public PositiveMapping(@Member("first") Resource first, @Member("second") Resource second,
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
		return new NegativeMapping(this.first, this.second, this.firstKnowledgeBase, this.secondKnowledgeBase,
				this.firstCategory, this.secondCategory);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PositiveMapping))
			return false;
		PositiveMapping other = (PositiveMapping) o;
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
	public PositiveMapping setCategories(String categorie) {
		this.firstCategory = Optional.ofNullable(categorie);
		this.secondCategory = Optional.ofNullable(categorie);
		return this;
	}

}