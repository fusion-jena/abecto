package de.uni_jena.cs.fusion.abecto;

import org.apache.jena.rdf.model.Resource;

/** Provides a representation of a {@link Number} and unit of measurement. */
public class Value {
	public final Number value;
	public final Resource unit;

	public Value(Number value, Resource unit) {
		this.value = value;
		this.unit = unit;
	}
}
