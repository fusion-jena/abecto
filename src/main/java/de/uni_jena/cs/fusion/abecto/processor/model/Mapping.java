package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "rdf", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public class Mapping {
	public static Mapping not() {
		return new Mapping(null, false, null, null, null, null, null, null);
	}

	public static Mapping not(Resource first, Resource second) {
		return new Mapping(null, false, first, second, null, null, null, null);
	}

	public static Mapping not(Resource first, Resource second, UUID firstKnowledgeBase, UUID secondKnowledgeBase,
			String categorie) {
		return new Mapping(null, false, first, second, Optional.ofNullable(secondKnowledgeBase),
				Optional.ofNullable(firstKnowledgeBase), Optional.ofNullable(categorie),
				Optional.ofNullable(categorie));
	}

	public static Mapping not(UUID firstKnowledgeBase, UUID secondKnowledgeBase, String categorie) {
		return new Mapping(null, false, null, null, Optional.ofNullable(secondKnowledgeBase),
				Optional.ofNullable(firstKnowledgeBase), Optional.ofNullable(categorie),
				Optional.ofNullable(categorie));
	}

	public static Mapping of() {
		return new Mapping(null, true, null, null, null, null, null, null);
	}

	public static Mapping of(Resource first, Resource second) {
		return new Mapping(null, true, first, second, null, null, null, null);
	}

	public static Mapping of(Resource first, Resource second, UUID firstKnowledgeBase, UUID secondKnowledgeBase,
			String categorie) {
		return new Mapping(null, true, first, second, Optional.ofNullable(firstKnowledgeBase),
				Optional.ofNullable(secondKnowledgeBase), Optional.ofNullable(categorie),
				Optional.ofNullable(categorie));
	}

	public static Mapping of(UUID firstKnowledgeBase, UUID secondKnowledgeBase, String categorie) {
		return new Mapping(null, true, null, null, Optional.ofNullable(firstKnowledgeBase),
				Optional.ofNullable(secondKnowledgeBase), Optional.ofNullable(categorie),
				Optional.ofNullable(categorie));
	}

	public static Mapping empty() {
		return new Mapping(null, null, null, null, null, null, null, null);
	}

	@SparqlPattern(predicate = "rdf:type", object = "abecto:Mapping")
	public Resource id;

	@SparqlPattern(subject = "id", predicate = "abecto:entitiesMap")
	public final Boolean entitiesMap;

	@SparqlPattern(subject = "id", predicate = "abecto:firstId")
	public final Resource first;

	@SparqlPattern(subject = "id", predicate = "abecto:secondId")
	public final Resource second;

	@SparqlPattern(subject = "id", predicate = "abecto:firstSourceKnowledgeBase")
	public Optional<UUID> firstKnowledgeBase;

	@SparqlPattern(subject = "id", predicate = "abecto:secondSourceKnowledgeBase")
	public Optional<UUID> secondKnowledgeBase;

	@SparqlPattern(subject = "id", predicate = "abecto:firstCategorisedAs")
	public Optional<String> firstCategory;

	@SparqlPattern(subject = "id", predicate = "abecto:secondCategorisedAs")
	public Optional<String> secondCategory;

	private final boolean switched;

	public Mapping(@Member("id") Resource id, @Member("entitiesMap") Boolean entitiesMap,
			@Member("first") Resource first, @Member("second") Resource second,
			@Member("firstKnowledgeBase") Optional<UUID> firstKnowledgeBase,
			@Member("secondKnowledgeBase") Optional<UUID> secondKnowledgeBase,
			@Member("firstCategory") Optional<String> firstCategory,
			@Member("secondCategory") Optional<String> secondCategory) {
		this.id = id;
		this.entitiesMap = entitiesMap;
		if (first == null || second != null && first.getURI().compareTo(second.getURI()) < 0) {
			this.switched = false;
			this.first = first;
			this.second = second;
			this.firstKnowledgeBase = firstKnowledgeBase;
			this.secondKnowledgeBase = secondKnowledgeBase;
			this.firstCategory = firstCategory;
			this.secondCategory = secondCategory;
		} else {
			this.switched = true;
			this.first = second;
			this.second = first;
			this.firstKnowledgeBase = secondKnowledgeBase;
			this.secondKnowledgeBase = firstKnowledgeBase;
			this.firstCategory = secondCategory;
			this.secondCategory = firstCategory;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Mapping))
			return false;
		Mapping other = (Mapping) o;
		return this.entitiesMap.equals(other.entitiesMap) && this.first.equals(other.first)
				&& this.second.equals(other.second);
	}

	@Override
	public int hashCode() {
		return this.first.getURI().hashCode() + this.second.getURI().hashCode() + (this.entitiesMap ? 1 : 0);
	}

	public Mapping inverse() {
		return new Mapping(null, !this.entitiesMap, this.first, this.second, this.firstKnowledgeBase,
				this.secondKnowledgeBase, this.firstCategory, this.secondCategory);
	}

	public Mapping setCategories(String categorie) {
		this.firstCategory = Optional.ofNullable(categorie);
		this.secondCategory = Optional.ofNullable(categorie);
		return this;
	}

	public Mapping setKnowledgeBases(UUID firstKnowledgeBase, UUID secondKnowledgeBase) {
		if (!this.switched) {
			this.firstKnowledgeBase = Optional.ofNullable(firstKnowledgeBase);
			this.secondKnowledgeBase = Optional.ofNullable(secondKnowledgeBase);
		} else {
			this.firstKnowledgeBase = Optional.ofNullable(secondKnowledgeBase);
			this.secondKnowledgeBase = Optional.ofNullable(firstKnowledgeBase);
		}
		return this;
	}

	public Resource getResourceOf(UUID knowledgeBase) {
		if (this.firstKnowledgeBase != null && knowledgeBase.equals(this.firstKnowledgeBase.get())) {
			return this.first;
		} else if (this.secondKnowledgeBase != null && knowledgeBase.equals(this.secondKnowledgeBase.get())) {
			return this.second;
		} else {
			throw new IllegalArgumentException("Unknown knowledge base.");
		}
	}
}
