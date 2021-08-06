package de.uni_jena.cs.fusion.abecto;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;

public class Plans {
	static Resource getPlan(Model configurationModel, Optional<String> planIri) {
		if (planIri.isPresent()) {
			Resource plan = ResourceFactory.createResource(planIri.get());
			if (!configurationModel.contains(plan, RDF.type, PPlan.Plan)) {
				throw new IllegalArgumentException(
						String.format("Selected plan %s not contained in the configuration.", planIri));
			}
			return plan;
		} else {
			try {
				return Models.assertOne(configurationModel.listSubjectsWithProperty(RDF.type, PPlan.Plan));
			} catch (NoSuchElementException e) {
				throw new IllegalArgumentException(String.format("Configuration does not contain a plan."));
			} catch (ToManyElementsException e) {
				throw new IllegalArgumentException(
						String.format("Configuration contains more than one plan, but no plan was selected."));
			}
		}
	}
}
