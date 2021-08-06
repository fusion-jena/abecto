package de.uni_jena.cs.fusion.abecto;

import static de.uni_jena.cs.fusion.abecto.util.Models.assertOne;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class Aspects {

	public static Map<Resource, Aspect> getAspects(Model configurationModel) {
		// init aspcet map
		Map<Resource, Aspect> aspects = new HashMap<>();
		// get aspects
		configurationModel.listSubjectsWithProperty(RDF.type, AV.Aspect)
				.forEach(aspect -> aspects.put(aspect, getAspect(configurationModel, aspect)));
		return aspects;
	}

	public static Aspect getAspect(Model configurationModel, Resource aspectName) {
		String keyVariableName = Models
				.assertOne(configurationModel.listObjectsOfProperty(aspectName, AV.keyVariableName)).asLiteral()
				.getString();

		Aspect aspect = new Aspect(aspectName, keyVariableName);

		// add patterns
		for (Resource aspectPatter : configurationModel.listSubjectsWithProperty(AV.ofAspect, aspectName).toList()) {
			Resource dataset = assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.associatedDataset))
					.asResource();
			Query pattern = (Query) assertOne(configurationModel.listObjectsOfProperty(aspectPatter, AV.definingQuery))
					.asLiteral().getValue();
			aspect.setPattern(dataset, pattern);
		}

		return aspect;
	}

}
