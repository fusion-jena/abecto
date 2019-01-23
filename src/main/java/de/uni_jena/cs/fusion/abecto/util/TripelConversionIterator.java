package de.uni_jena.cs.fusion.abecto.util;

import java.util.Iterator;

import org.apache.jena.graph.Triple;
import org.rdfhdt.hdt.triples.TripleString;

public class TripelConversionIterator implements Iterator<TripleString> {

	private final Iterator<Triple> tripleIterator;

	public TripelConversionIterator(Iterator<Triple> tripleIterator) {
		this.tripleIterator = tripleIterator;
	}

	@Override
	public boolean hasNext() {
		return tripleIterator.hasNext();
	}

	@Override
	public TripleString next() {
		Triple t = tripleIterator.next();
		return new TripleString(t.getSubject().toString(null, false), t.getPredicate().toString(null, false),
				t.getObject().toString(null, false));
	}

}
