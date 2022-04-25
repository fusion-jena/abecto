package de.uni_jena.cs.fusion.abecto.util;

import java.util.Arrays;
import java.util.Objects;

import org.apache.jena.rdf.model.RDFNode;

public class Values {
	private final RDFNode[] valuesArray;

	public Values(RDFNode[] valuesArray) {
		this.valuesArray = valuesArray;
	}

	@Override
	public int hashCode() {
		return Arrays.stream(valuesArray).filter(Objects::nonNull).mapToInt(Object::hashCode).reduce((a, b) -> a ^ b)
				.orElse(0);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof Values && Arrays.equals(((Values) obj).valuesArray, this.valuesArray);
	}

	public RDFNode[] getValuesArray() {
		return Arrays.copyOf(valuesArray, valuesArray.length);
	}

}
