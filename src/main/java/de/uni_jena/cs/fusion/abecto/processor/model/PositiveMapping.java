package de.uni_jena.cs.fusion.abecto.processor.model;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class PositiveMapping implements Mapping {
	public final Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:equivalent")
	public final Resource second;

	public final static PositiveMapping prototype = new PositiveMapping(null, null);

	private final static int HASH_OFFSET = 0;

	public PositiveMapping(@Member("first") Resource first, @Member("second") Resource second) {
		this.first = first;
		this.second = second;
	}

	public Mapping inverse() {
		return new NegativeMapping(this.first, this.second);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PositiveMapping)) return false;
		PositiveMapping other = (PositiveMapping) o;
		return this.first.equals(other.first) && this.second.equals(other.second)
				|| this.first.equals(other.second) && this.second.equals(other.first);
	}

	@Override
	public int hashCode() {
		return first.getURI().hashCode() + second.getURI().hashCode() + HASH_OFFSET;
	}

}