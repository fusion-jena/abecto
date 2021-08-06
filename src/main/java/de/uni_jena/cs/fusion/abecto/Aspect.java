package de.uni_jena.cs.fusion.abecto;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;

public class Aspect {
	public final Resource name;
	private final String keyVariableName;
	private final Map<Resource, Query> patternByDataset = new HashMap<>();

	public Aspect(Resource name, String keyVariableName) {
		this.name = name;
		this.keyVariableName = keyVariableName;
	}

	public void setPattern(Resource dataset, Query pattern) {
		patternByDataset.put(dataset, pattern);
	}

	public Query getPattern(Resource dataset) {
		return patternByDataset.get(dataset);
	}

	public String getKeyVariableName() {
		return keyVariableName;
	}

}
