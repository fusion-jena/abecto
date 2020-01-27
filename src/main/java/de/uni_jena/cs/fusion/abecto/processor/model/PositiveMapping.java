package de.uni_jena.cs.fusion.abecto.processor.model;

import java.util.Objects;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class PositiveMapping implements Mapping {
	public Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:equivalent")
	public Resource second;

	private final static int HASH_OFFSET = 0;

	public PositiveMapping() {
	}

	public PositiveMapping(Resource first, Resource second) {
		this.first = first;
		this.second = second;
	}

	public Mapping inverse() {
		return new NegativeMapping(this.first, this.second);
	}

	@Override
	public boolean equals(Object o) {
		PositiveMapping other = (PositiveMapping) o;
		return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second)
				|| Objects.equals(this.first, other.second) && Objects.equals(this.second, other.first);
	}

	@Override
	public int hashCode() {
		return ((this.first != null) ? this.first.getURI().hashCode() : 0)
				+ ((this.second != null) ? this.second.getURI().hashCode() : 0) + HASH_OFFSET;
	}

}