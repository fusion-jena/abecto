package de.uni_jena.cs.fusion.abecto.processor.model;

import org.apache.jena.rdf.model.Resource;

import de.uni_jena.cs.fusion.abecto.sparq.Member;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

@SparqlNamespace(prefix = "abecto", namespace = "http://fusion.cs.uni-jena.de/ontology/abecto/")
public final class NegativeMapping implements Mapping {
	public final Resource first;
	@SparqlPattern(subject = "first", predicate = "abecto:different")
	public final Resource second;

	public final static NegativeMapping prototype = new NegativeMapping(null, null);

	private final static int HASH_OFFSET = 1;

	public NegativeMapping(@Member("first") Resource first, @Member("second") Resource second) {
		this.first = first;
		this.second = second;
	}

	public Mapping inverse() {
		return new PositiveMapping(this.first, this.second);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NegativeMapping)) return false;
		NegativeMapping other = (NegativeMapping) o;
		return this.first.equals(other.first) && this.second.equals(other.second)
				|| this.first.equals(other.second) && this.second.equals(other.first);
	}

	@Override
	public int hashCode() {
		return first.getURI().hashCode() + second.getURI().hashCode() + HASH_OFFSET;
	}

}