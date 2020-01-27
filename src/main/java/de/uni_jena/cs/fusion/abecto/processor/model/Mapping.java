package de.uni_jena.cs.fusion.abecto.processor.model;

import org.apache.jena.rdf.model.Resource;

public interface Mapping {

	public static PositiveMapping of(Resource first, Resource second) {
		return new PositiveMapping(first, second);
	}

	public static NegativeMapping not(Resource first, Resource second) {
		return new NegativeMapping(first, second);
	}

	public Mapping inverse();
}
