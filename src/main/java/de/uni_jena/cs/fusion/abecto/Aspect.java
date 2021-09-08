package de.uni_jena.cs.fusion.abecto;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

public class Aspect {
	public final Resource iri;
	private final String keyVariableName;
	private final Var keyVariable;
	private final Map<Resource, Query> patternByDataset = new HashMap<>();

	public Aspect(Resource iri, String keyVariableName) {
		this.iri = iri;
		this.keyVariableName = keyVariableName;
		this.keyVariable = Var.alloc(keyVariableName);
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

	public Var getKeyVariable() {
		return keyVariable;
	}

}
