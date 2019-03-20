package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Var;

import de.uni_jena.cs.fusion.abecto.util.Vocabulary;
import de.uni_jena.cs.fusion.similarity.jarowinkler.JaroWinklerSimilarity;

public class JaroWinklerMappingProcessor extends AbstractMappingProcessor {

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Map.of("threshold", new TypeLiteral<Double>() {
		}, "case_sensitive", new TypeLiteral<Boolean>() {
		}, "property", new TypeLiteral<String>() {
		});
	}

	private Map<String, Collection<Node_URI>> getLabels(Graph graph, Node property, boolean caseSensitive) {
		SelectBuilder selectBuilder = new SelectBuilder().addVar("?e").addVar("?l").addWhere("?e", "?p", "?l");
		selectBuilder.setVar(Var.alloc("?p"), property);
		Query query = selectBuilder.build();
		Model model = ModelFactory.createModelForGraph(graph);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet results = queryExecution.execSelect();
		Map<String, Collection<Node_URI>> labels = new HashMap<>();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Node entity = result.get("e").asNode();
			if (!entity.isURI()) {
				continue;
			}
			String label = result.getLiteral("l").getString();
			if (caseSensitive) {
				label = label.toLowerCase();
			}
			labels.putIfAbsent(label, new HashSet<Node_URI>());
			labels.get(label).add((Node_URI) entity);
		}
		return labels;
	}

	@Override
	protected Collection<Mapping> computeMapping(Graph firstGraph, Graph secondGraph) {
		boolean caseSensitive = this.getProperty("case_sensitive", new TypeLiteral<Boolean>() {
		});
		double threshold = this.getProperty("threshold", new TypeLiteral<Double>() {
		});
		Node property = this.getOptionalProperty("property", new TypeLiteral<String>() {
		}, NodeFactory::createURI).orElse(Vocabulary.RDFS_LABEL);

		// get label maps
		Map<String, Collection<Node_URI>> firstGraphLabels = getLabels(firstGraph, property, caseSensitive);
		Map<String, Collection<Node_URI>> secondGraphLabels = getLabels(secondGraph, property, caseSensitive);

		// swap if second label map is larger
		if (firstGraphLabels.size() < secondGraphLabels.size()) {
			Map<String, Collection<Node_URI>> tmp = firstGraphLabels;
			firstGraphLabels = secondGraphLabels;
			secondGraphLabels = tmp;
		}

		// prepare JaroWinklerSimilarity instance using larger label map
		JaroWinklerSimilarity<Collection<Node_URI>> jws = JaroWinklerSimilarity.with(firstGraphLabels, threshold);

		// prepare mappings collection
		Collection<Mapping> mappings = new ArrayList<>();

		// iterate smaller label map
		for (String label : secondGraphLabels.keySet()) {
			// get best matches
			Map<Collection<Node_URI>, Double> searchResult = jws.apply(label);
			Set<Node_URI> matchingNodes = new HashSet<>();
			double maxSimilarity = 0d;
			for (Entry<Collection<Node_URI>, Double> entry : searchResult.entrySet()) {
				if (entry.getValue() > maxSimilarity) {
					matchingNodes.clear();
					maxSimilarity = entry.getValue();
					matchingNodes.addAll(entry.getKey());
				} else if (entry.getValue().equals(maxSimilarity)) {
					matchingNodes.addAll(entry.getKey());
				} else {
					// do nothing
				}
			}
			// convert matches into mappings
			for (Node_URI node : secondGraphLabels.get(label)) {
				for (Node_URI matchingNode : matchingNodes) {
					mappings.add(Mapping.of(node, matchingNode));
				}
			}

		}

		return mappings;
	}
}