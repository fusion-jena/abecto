/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.uni_jena.cs.fusion.abecto.util.Models;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.SdmxAttribute;

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

	public static boolean containsDeviation(Resource affectedResource, String affectedVariableName,
			RDFNode affectedValue, Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue,
			Resource affectedAspect, Model outputAffectedDatasetMetaModel) {

		Var deviation = Var.alloc("deviation");
		Var qualityAnnotation = Var.alloc("qualityAnnotation");

		AskBuilder builder = new AskBuilder();
		builder.addWhere(deviation, RDF.type, AV.Deviation);
		builder.addWhere(deviation, AV.affectedAspect, affectedAspect);
		builder.addWhere(deviation, AV.affectedVariableName, affectedVariableName);
		builder.addWhere(deviation, AV.affectedValue, affectedValue);
		builder.addWhere(deviation, AV.comparedToDataset, comparedToDataset);
		builder.addWhere(deviation, AV.comparedToResource, comparedToResource);
		builder.addWhere(deviation, AV.comparedToValue, comparedToValue);
		builder.addWhere(qualityAnnotation, RDF.type, DQV.QualityAnnotation);
		builder.addWhere(qualityAnnotation, OA.hasTarget, affectedResource);
		builder.addWhere(qualityAnnotation, OA.hasBody, deviation);

		return QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execAsk();
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

	public static boolean containsResourceOmission(Resource affectedDataset, Resource comparedToDataset,
			Resource comparedToResource, Resource affectedAspect, Model outputAffectedDatasetMetaModel) {

		Var resourceOmission = Var.alloc("resourceOmission");
		Var qualityAnnotation = Var.alloc("qualityAnnotation");

		AskBuilder builder = new AskBuilder();
		builder.addWhere(resourceOmission, RDF.type, AV.ResourceOmission);
		builder.addWhere(resourceOmission, AV.affectedAspect, affectedAspect);
		builder.addWhere(resourceOmission, AV.comparedToDataset, comparedToDataset);
		builder.addWhere(resourceOmission, AV.comparedToResource, comparedToResource);
		builder.addWhere(qualityAnnotation, RDF.type, DQV.QualityAnnotation);
		builder.addWhere(qualityAnnotation, OA.hasTarget, affectedDataset);
		builder.addWhere(qualityAnnotation, OA.hasBody, resourceOmission);

		return QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execAsk();
	}

	public static boolean ask(Model model, String query) {
		return QueryExecutionFactory.create(QueryFactory.create(query), model).execAsk();
	}

	public static int selectOneInt(Model model, String query, String varName) {
		return Models.assertOne(QueryExecutionFactory.create(QueryFactory.create(query), model).execSelect())
				.getLiteral(varName).getInt();
	}

	public static boolean containsValuesOmission(Resource affectedResource, String affectedVariableName,
			Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue, Resource affectedAspect,
			Model outputAffectedDatasetMetaModel) {

		Var valueOmission = Var.alloc("resourceOmission");
		Var qualityAnnotation = Var.alloc("qualityAnnotation");

		AskBuilder builder = new AskBuilder();
		builder.addWhere(valueOmission, RDF.type, AV.ValueOmission);
		builder.addWhere(valueOmission, AV.affectedAspect, affectedAspect);
		builder.addWhere(valueOmission, AV.affectedVariableName, affectedVariableName);
		builder.addWhere(valueOmission, AV.comparedToDataset, comparedToDataset);
		builder.addWhere(valueOmission, AV.comparedToResource, comparedToResource);
		builder.addWhere(valueOmission, AV.comparedToValue, comparedToValue);
		builder.addWhere(qualityAnnotation, RDF.type, DQV.QualityAnnotation);
		builder.addWhere(qualityAnnotation, OA.hasTarget, affectedResource);
		builder.addWhere(qualityAnnotation, OA.hasBody, valueOmission);

		return QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execAsk();
	}

	public static boolean containsMeasurement(Resource measure, @Nullable Number value, Resource unit,
			Resource computedOnDataset, @Nullable String affectedVariableName, Iterable<Resource> comparedToDatasets,
			Resource affectedAspect, Model outputAffectedDatasetMetaModel) {

		Var qualityMeasurement = Var.alloc("qualityMeasurement");

		AskBuilder builder = new AskBuilder();
		builder.addWhere(qualityMeasurement, RDF.type, AV.QualityMeasurement);
		builder.addWhere(qualityMeasurement, DQV.isMeasurementOf, measure);
		builder.addWhere(qualityMeasurement, DQV.computedOn, computedOnDataset);
		if (value != null) {
			builder.addWhere(qualityMeasurement, DQV.value, value);
		}
		builder.addWhere(qualityMeasurement, SdmxAttribute.unitMeasure, unit);
		builder.addWhere(qualityMeasurement, AV.affectedAspect, affectedAspect);
		if (affectedVariableName != null) {
			builder.addWhere(qualityMeasurement, AV.affectedVariableName, affectedVariableName);
		}
		for (Resource comparedToDataset : comparedToDatasets) {
			builder.addWhere(qualityMeasurement, AV.comparedToDataset, comparedToDataset);
		}

		return QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execAsk();
	}

	public static BigDecimal getMeasurement(Resource measure, Resource unit, Resource computedOnDataset,
			@Nullable String affectedVariableName, @Nullable Iterable<Resource> comparedToDatasets,
			Resource affectedAspect, Model outputAffectedDatasetMetaModel) {

		Var qualityMeasurement = Var.alloc("qualityMeasurement");
		Var value = Var.alloc("value");

		SelectBuilder builder = new SelectBuilder();
		builder.addVar(value);
		builder.addWhere(qualityMeasurement, RDF.type, AV.QualityMeasurement);
		builder.addWhere(qualityMeasurement, DQV.isMeasurementOf, measure);
		builder.addWhere(qualityMeasurement, DQV.computedOn, computedOnDataset);
		if (value != null) {
			builder.addWhere(qualityMeasurement, DQV.value, value);
		}
		builder.addWhere(qualityMeasurement, SdmxAttribute.unitMeasure, unit);
		builder.addWhere(qualityMeasurement, AV.affectedAspect, affectedAspect);
		if (affectedVariableName != null) {
			builder.addWhere(qualityMeasurement, AV.affectedVariableName, affectedVariableName);
		}
		if (comparedToDatasets != null) {
			for (Resource comparedToDataset : comparedToDatasets) {
				builder.addWhere(qualityMeasurement, AV.comparedToDataset, comparedToDataset);
			}
		}

		return new BigDecimal(Models
				.assertOne(QueryExecutionFactory.create(builder.build(), outputAffectedDatasetMetaModel).execSelect())
				.getLiteral(value.getName()).getLexicalForm());
	}

}
