/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.SdmxAttribute;

public class Metadata {

	public static void addDeviation(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
			Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
		Resource deviation = outputAffectedDatasetMetaModel.createResource(AV.Deviation);
		deviation.addProperty(AV.affectedAspect, affectedAspect.iri);
		deviation.addLiteral(AV.affectedVariableName, affectedVariableName);
		deviation.addLiteral(AV.affectedValue, affectedValue);
		deviation.addProperty(AV.comparedToDataset, comparedToDataset);
		deviation.addProperty(AV.comparedToResource, comparedToResource);
		deviation.addLiteral(AV.comparedToValue, comparedToValue);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, deviation);
	}

	public static void addIssue(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
			Resource affectedAspect, String issueType, String comment, Model outputAffectedDatasetMetaModel) {
		Resource issue = outputAffectedDatasetMetaModel.createResource(AV.Issue);
		issue.addProperty(AV.affectedAspect, affectedAspect);
		issue.addLiteral(AV.affectedVariableName, affectedVariableName);
		issue.addLiteral(AV.affectedValue, affectedValue);
		issue.addLiteral(AV.issueType, issueType);
		issue.addLiteral(RDFS.comment, comment);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, issue);
	}

	public static void addResourceOmission(Resource affectedDataset, Resource comparedToDataset,
			Resource comparedToResource, Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		Resource resourceOmission = outputAffectedDatasetMetaModel.createResource(AV.ResourceOmission);
		resourceOmission.addProperty(AV.affectedAspect, affectedAspect.iri);
		resourceOmission.addProperty(AV.comparedToDataset, comparedToDataset);
		resourceOmission.addProperty(AV.comparedToResource, comparedToResource);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedDataset);
		qualityAnnotation.addProperty(OA.hasBody, resourceOmission);
	}

	public static void addValuesOmission(Resource affectedResource, String affectedVariableName,
			Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
		Resource valuesOmission = outputAffectedDatasetMetaModel.createResource(AV.ValueOmission);
		valuesOmission.addProperty(AV.affectedAspect, affectedAspect.iri);
		valuesOmission.addLiteral(AV.affectedVariableName, affectedVariableName);
		valuesOmission.addProperty(AV.comparedToDataset, comparedToDataset);
		valuesOmission.addProperty(AV.comparedToResource, comparedToResource);
		valuesOmission.addLiteral(AV.comparedToValue, comparedToValue);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, valuesOmission);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		addQualityMeasurement(measure, value, unit, computedOnDataset, null, Collections.emptyList(), affectedAspect,
				outputAffectedDatasetMetaModel);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			String affectedVariableName, Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		addQualityMeasurement(measure, value, unit, computedOnDataset, affectedVariableName, Collections.emptyList(),
				affectedAspect, outputAffectedDatasetMetaModel);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			Resource comparedToDataset, Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		addQualityMeasurement(measure, value, unit, computedOnDataset, null,
				Collections.singletonList(comparedToDataset), affectedAspect, outputAffectedDatasetMetaModel);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			String affectedVariableName, Resource comparedToDataset, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
		addQualityMeasurement(measure, value, unit, computedOnDataset, affectedVariableName,
				Collections.singletonList(comparedToDataset), affectedAspect, outputAffectedDatasetMetaModel);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			Iterable<Resource> comparedToDatasets, Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		addQualityMeasurement(measure, value, unit, computedOnDataset, null, comparedToDatasets, affectedAspect,
				outputAffectedDatasetMetaModel);
	}

	public static void addQualityMeasurement(Resource measure, Number value, Resource unit, Resource computedOnDataset,
			@Nullable String affectedVariableName, Iterable<Resource> comparedToDatasets, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
		Resource qualityMeasurement = outputAffectedDatasetMetaModel.createResource(AV.QualityMeasurement);
		qualityMeasurement.addProperty(DQV.isMeasurementOf, measure);
		qualityMeasurement.addLiteral(DQV.value, value);
		qualityMeasurement.addProperty(SdmxAttribute.unitMeasure, unit);
		qualityMeasurement.addProperty(AV.affectedAspect, affectedAspect.iri);
		if (affectedVariableName != null) {
			qualityMeasurement.addLiteral(AV.affectedVariableName, affectedVariableName);
		}
		for (Resource comparedToDataset : comparedToDatasets) {
			qualityMeasurement.addProperty(AV.comparedToDataset, comparedToDataset);
		}
	}

	private static final Var QUALITY_ANNOTATION = Var.alloc("qualityAnnotation");
	private static final Var QUALITY_ANNOTATION_BODY = Var.alloc("qualityAnnotationBody");
	private static final Var AFFECTED_VARIABLE_NAME = Var.alloc("affectedVariableName");
	private static final Var AFFECTED_VALUE = Var.alloc("affectedValue");
	private static final Var AFFECTED_RESOURCE = Var.alloc("affectedResource");
	private static final Var AFFECTED_ASPECT = Var.alloc("affectedAspect");

	private static final Query IS_WRONG_VALUE_QUERY = new AskBuilder()
			.addWhere(QUALITY_ANNOTATION, OA.hasTarget, AFFECTED_RESOURCE)
			.addWhere(QUALITY_ANNOTATION, OA.hasBody, QUALITY_ANNOTATION_BODY)
			.addWhere(QUALITY_ANNOTATION_BODY, RDF.type, AV.WrongValue)
			.addWhere(QUALITY_ANNOTATION_BODY, AV.affectedAspect, AFFECTED_ASPECT)
			.addWhere(QUALITY_ANNOTATION_BODY, AV.affectedValue, AFFECTED_VALUE)
			.addWhere(QUALITY_ANNOTATION_BODY, AV.affectedVariableName, AFFECTED_VARIABLE_NAME).build();

	/**
	 * Checks if a value is known to be wrong.
	 * 
	 * @param affectedResource              the resource the value belongs to
	 * @param affectedVariableName          the variable the value belongs to
	 * @param affectedValue                 the value to check
	 * @param affectedAspect                the aspect the resource belongs to
	 * @param inputAffectedDatasetMetaModel the model containing information about
	 *                                      wrong values
	 * @return {@code true} if the value is known to be wrong, otherwise
	 *         {@code false}
	 */
	public static boolean isWrongValue(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
			Resource affectedAspect, Model inputAffectedDatasetMetaModel) {
		Query query = AskBuilder.rewrite(IS_WRONG_VALUE_QUERY.cloneQuery(),
				Map.of(AFFECTED_RESOURCE, affectedResource.asNode(), //
						AFFECTED_VARIABLE_NAME, NodeFactory.createLiteral(affectedVariableName), //
						AFFECTED_VALUE, affectedValue.asNode(), //
						AFFECTED_ASPECT, affectedAspect.asNode()));
		return QueryExecutionFactory.create(query, inputAffectedDatasetMetaModel).execAsk();

	}
}
