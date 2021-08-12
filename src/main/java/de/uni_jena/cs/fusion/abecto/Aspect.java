package de.uni_jena.cs.fusion.abecto;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;

public class Aspect {
	public final Resource name;
	private final String keyVariableName;
	private final Var keyVariable;
	private final Map<Resource, Query> patternByDataset = new HashMap<>();

	public Aspect(Resource name, String keyVariableName) {
		this.name = name;
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

	public Optional<Map<String, Set<RDFNode>>> getResource(Resource dataset, Resource keyValue, Model datasetModels) {
		Query query = SelectBuilder.rewrite(patternByDataset.get(dataset).cloneQuery(),
				Collections.singletonMap(keyVariable, keyValue.asNode()));
		ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();
		if (results.hasNext()) {
			Map<String, Set<RDFNode>> values = new HashMap<>();
			for (String varName : results.getResultVars()) {
				if (!varName.equals(keyVariableName)) {
					values.put(varName, new HashSet<>());
				}
			}
			while (results.hasNext()) {
				QuerySolution result = results.next();
				for (Entry<String, Set<RDFNode>> entry : values.entrySet()) {
					entry.getValue().add(result.get(entry.getKey()));
				}
			}
			return Optional.of(values);
		} else {
			return Optional.empty();
		}
	}

}
