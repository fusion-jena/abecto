package de.uni_jena.cs.fusion.abecto;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;

public class TestUtil {
	public final static String namespace = "http://example.org/";

	public static Resource aspect(int i) {
		return ResourceFactory.createProperty(namespace + "aspect" + i);
	}

	public static Resource dataset(int i) {
		return ResourceFactory.createResource(namespace + "dataset" + i);
	}

	public static Property property(int i) {
		return ResourceFactory.createProperty(namespace + "property" + i);
	}

	public static Resource subject(int i) {
		return ResourceFactory.createResource(namespace + "subject" + i);
	}

	public static Resource object(int i) {
		return ResourceFactory.createResource(namespace + "object" + i);
	}

	public static Resource resource(int i) {
		return ResourceFactory.createResource(namespace + "resource" + i);
	}

	public static Resource resource(String local) {
		return ResourceFactory.createResource(namespace + local);
	}

	public static boolean containsIssue(Resource affectedResource, @Nullable String affectedVariableName,
			@Nullable RDFNode affectedValue, Resource affectedAspect, @Nullable String issueType, String comment,
			Model outputAffectedDatasetMetaModel) {

		Var issue = Var.alloc("issue");
		Var qualityAnnotation = Var.alloc("qualityAnnotation");

		AskBuilder builder = new AskBuilder();
		builder.addWhere(issue, RDF.type, AV.Issue);
		builder.addWhere(issue, AV.affectedAspect, affectedAspect);
		if (affectedVariableName != null) {
			builder.addWhere(issue, AV.affectedVariableName, affectedVariableName);
		}
		if (affectedVariableName != null) {
			builder.addWhere(issue, AV.affectedValue, affectedValue);
		}
		builder.addWhere(issue, AV.issueType, issueType);
		if (affectedVariableName != null) {
			builder.addWhere(issue, RDFS.comment, comment);
		}
		builder.addWhere(qualityAnnotation, RDF.type, DQV.QualityAnnotation);
		builder.addWhere(qualityAnnotation, OA.hasTarget, affectedResource);
		builder.addWhere(qualityAnnotation, OA.hasBody, issue);

		return QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execAsk();
	}

}
