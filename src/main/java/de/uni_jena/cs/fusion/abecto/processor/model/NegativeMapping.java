package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Objects;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class NegativeMapping implements Mapping {
	public Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:different")
	public Resource second;

	private final static int HASH_OFFSET = 1;

	public NegativeMapping() {
	}

	public NegativeMapping(Resource first, Resource second) {
		this.first = first;
		this.second = second;
	}

	public Mapping inverse() {
		return new PositiveMapping(this.first, this.second);
	}

	@Override
	public boolean equals(Object o) {
		NegativeMapping other = (NegativeMapping) o;
		return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second)
				|| Objects.equals(this.first, other.second) && Objects.equals(this.second, other.first);
	}

	@Override
	public int hashCode() {
		return ((first != null) ? first.getURI().hashCode() : 0) + ((second != null) ? second.getURI().hashCode() : 0)
				+ HASH_OFFSET;
	}

}