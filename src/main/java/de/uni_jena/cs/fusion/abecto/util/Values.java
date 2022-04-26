package de.uni_jena.cs.fusion.abecto.util;

import java.util.Arrays;

import org.apache.jena.rdf.model.RDFNode;

public class Values {
	private final RDFNode[] valuesArray;

	public Values(RDFNode[] valuesArray) {
		this.valuesArray = valuesArray;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (RDFNode node : valuesArray) {
			if (node != null) {
				hash ^= node.hashCode();
			}
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof Values && Arrays.equals(((Values) obj).valuesArray, this.valuesArray);
	}

	public RDFNode[] getValuesArray() {
		return Arrays.copyOf(valuesArray, valuesArray.length);
	}

}
