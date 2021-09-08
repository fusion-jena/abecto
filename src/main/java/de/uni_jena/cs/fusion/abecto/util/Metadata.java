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
package de.uni_jena.cs.fusion.abecto.util;

import java.util.Map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OA;

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
		Resource deviation = outputAffectedDatasetMetaModel.createResource(AV.Issue);
		deviation.addProperty(AV.affectedAspect, affectedAspect);
		deviation.addLiteral(AV.affectedVariableName, affectedVariableName);
		deviation.addLiteral(AV.affectedValue, affectedValue);
		deviation.addLiteral(AV.issueType, issueType);
		deviation.addLiteral(RDFS.comment, comment);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, deviation);
	}

	public static void addResourceOmission(Resource affectedDataset, Resource comparedToDataset,
			Resource comparedToResource, Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		Resource omission = outputAffectedDatasetMetaModel.createResource(AV.ResourceOmission);
		omission.addProperty(AV.affectedAspect, affectedAspect.iri);
		omission.addProperty(AV.comparedToDataset, comparedToDataset);
		omission.addProperty(AV.comparedToResource, comparedToResource);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedDataset);
		qualityAnnotation.addProperty(OA.hasBody, omission);
	}

	public static void addValuesOmission(Resource affectedResource, String affectedVariableName,
			Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
		Resource deviation = outputAffectedDatasetMetaModel.createResource(AV.ValueOmission);
		deviation.addProperty(AV.affectedAspect, affectedAspect.iri);
		deviation.addLiteral(AV.affectedVariableName, affectedVariableName);
		deviation.addProperty(AV.comparedToDataset, comparedToDataset);
		deviation.addProperty(AV.comparedToResource, comparedToResource);
		deviation.addLiteral(AV.comparedToValue, comparedToValue);
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, deviation);
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
