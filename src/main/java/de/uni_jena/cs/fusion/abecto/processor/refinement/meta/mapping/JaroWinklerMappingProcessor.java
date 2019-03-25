package de.uni_jena.cs.fusion.abecto.processor.refinement.meta.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

import de.uni_jena.cs.fusion.abecto.util.Vocabulary;
import de.uni_jena.cs.fusion.similarity.jarowinkler.JaroWinklerSimilarity;

public class JaroWinklerMappingProcessor extends AbstractMappingProcessor {

	// TODO add language handling parameter

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		return Map.of("threshold", new TypeLiteral<Double>() {}, "case_sensitive", new TypeLiteral<Boolean>() {},
				"property", new TypeLiteral<String>() {});
	}

	private Map<String, Collection<Resource>> getLabels(Model model, Property property, boolean caseSensitive) {
		Iterator<Statement> labelStatements = this.metaModel.listStatements(null, property, (String) null);
		Map<String, Collection<Resource>> labels = new HashMap<>();
		while (labelStatements.hasNext()) {
			Statement statement = labelStatements.next();
			Resource resource = statement.getSubject();
			String label = statement.getObject().asLiteral().getString();
			if (caseSensitive) {
				label = label.toLowerCase();
			}
			labels.computeIfAbsent(label, (a) -> new HashSet<Resource>()).add(resource);
		}
		return labels;
	}

	@Override
	protected Collection<Mapping> computeMapping(Model firstModel, Model secondModel) {
		boolean caseSensitive = this.getProperty("case_sensitive", new TypeLiteral<Boolean>() {});
		double threshold = this.getProperty("threshold", new TypeLiteral<Double>() {});
		Property property = this
				.getOptionalProperty("property", new TypeLiteral<String>() {}, ResourceFactory::createProperty)
				.orElse(Vocabulary.RDFS_LABEL);

		// get label maps
		Map<String, Collection<Resource>> firstModelLabels = getLabels(firstModel, property, caseSensitive);
		Map<String, Collection<Resource>> secondModelLabels = getLabels(secondModel, property, caseSensitive);

		// swap if second label map is larger
		if (firstModelLabels.size() < secondModelLabels.size()) {
			Map<String, Collection<Resource>> tmp = firstModelLabels;
			firstModelLabels = secondModelLabels;
			secondModelLabels = tmp;
		}

		// prepare JaroWinklerSimilarity instance using larger label map
		JaroWinklerSimilarity<Collection<Resource>> jws = JaroWinklerSimilarity.with(firstModelLabels, threshold);

		// prepare mappings collection
		Collection<Mapping> mappings = new ArrayList<>();

		// iterate smaller label map
		for (String label : secondModelLabels.keySet()) {
			// get best matches
			Map<Collection<Resource>, Double> searchResult = jws.apply(label);
			Set<Resource> matchingResources = new HashSet<>();
			double maxSimilarity = 0d;
			for (Entry<Collection<Resource>, Double> entry : searchResult.entrySet()) {
				if (entry.getValue() > maxSimilarity) {
					matchingResources.clear();
					maxSimilarity = entry.getValue();
					matchingResources.addAll(entry.getKey());
				} else if (entry.getValue().equals(maxSimilarity)) {
					matchingResources.addAll(entry.getKey());
				} else {
					// do nothing
				}
			}
			// convert matches into mappings
			for (Resource resource : secondModelLabels.get(label)) {
				for (Resource matchingResource : matchingResources) {
					mappings.add(Mapping.of(resource, matchingResource));
				}
			}

		}

		return mappings;
	}
}