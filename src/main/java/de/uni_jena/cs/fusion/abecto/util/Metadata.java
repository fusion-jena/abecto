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

import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OA;

public class Metadata {

	private static AskBuilder CORRESPONDENCE_EXISTS_OR_CONTRADICTS_QUERY = new AskBuilder()
			.addWhere(Var.alloc("set"), RDF.type, Var.alloc("setType"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addWhere(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"));

	/**
	 * Check if a correspondence or incorrespondence for two given resources in a
	 * given aspect already exist.
	 * <p>
	 * <strong>Note:<strong> The use of this method is not mandatory, as it will
	 * also be called by {@link #addCorrespondence(Resource, Resource, Aspect)} and
	 * {@link #addIncorrespondence(Resource, Resource, Aspect)}.
	 * 
	 * @param resource1  first resource to check
	 * @param resource2  second resource to check
	 * @param aspect     aspect affected by the (in)correspondence
	 * @param inputModel model containing already known (in)correspondences
	 * @return {@code true}, if an equivalent or contradicting (in)correspondence
	 *         exists, otherwise {@code false}
	 */
	public static boolean existsOrContradicts(Resource resource1, Resource resource2, Aspect aspect, Model inputModel) {
		AskBuilder queryBuilder = CORRESPONDENCE_EXISTS_OR_CONTRADICTS_QUERY.clone();
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		return QueryExecutionFactory.create(queryBuilder.build(), inputModel).execAsk();
	}

	private static ConstructBuilder ADD_CORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(Var.alloc("set"), RDF.type, Var.alloc("setType"))
			.addConstruct(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addConstruct(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addConstruct(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"));

	private static void add(Resource resource1, Resource resource2, Aspect aspect, boolean incorrespondence,
			Model inputModel, Model outputModel) {
		ConstructBuilder queryBuilder = ADD_CORRESPONDENCE_QUERY.clone();
		queryBuilder.setVar(Var.alloc("setType"), incorrespondence ? AV.IncorrespondenceSet : AV.CorrespondenceSet);
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		QueryExecutionFactory.create(queryBuilder.build(), outputModel).execConstruct(outputModel);
	}

	private static ConstructBuilder ADD_TRANSITIVE_CORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(Var.alloc("set"), AV.containdResource, Var.alloc("resourceTransitive"))
			.addWhere(Var.alloc("set"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addWhere(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"))
			.addWhere(Var.alloc("otherSet"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resourceTransitive"))
			.addWhere(Var.alloc("otherSet"), AV.affectedAspect, Var.alloc("aspect"))//
			.addWhere(new WhereBuilder()//
					.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resource1"))
					.addUnion(new WhereBuilder()//
							.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resource2"))));

	private static ConstructBuilder ADD_TRANSITIVE_INCORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(Var.alloc("transitiveSet"), RDF.type, AV.IncorrespondenceSet)
			.addConstruct(Var.alloc("transitiveSet"), AV.containdResource, Var.alloc("resourceTransitive1"))
			.addConstruct(Var.alloc("transitiveSet"), AV.containdResource, Var.alloc("resourceTransitive2"))
			.addConstruct(Var.alloc("transitiveSet"), AV.affectedAspect, Var.alloc("aspect"))
			.addWhere(Var.alloc("otherSet"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resourceTransitive2"))
			.addWhere(Var.alloc("otherSet"), AV.affectedAspect, Var.alloc("aspect"))//
			.addWhere(new WhereBuilder()//
					.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resource1"))
					.addBind(new ExprVar(Var.alloc("resource2")), Var.alloc("resourceTransitive1"))
					.addUnion(new WhereBuilder()//
							.addWhere(Var.alloc("otherSet"), AV.containdResource, Var.alloc("resource2"))
							.addBind(new ExprVar(Var.alloc("resource1")), Var.alloc("resourceTransitive1"))));

	private static void addTransitiveInCorrespondence(Resource resource1, Resource resource2, Aspect aspect,
			boolean incorrespondence, Model inputModel, Model outputModel) {
		ConstructBuilder queryBuilder = (incorrespondence ? ADD_TRANSITIVE_INCORRESPONDENCE_QUERY
				: ADD_TRANSITIVE_CORRESPONDENCE_QUERY).clone();
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		QueryExecutionFactory.create(queryBuilder.build(), inputModel).execConstruct(outputModel);
	}

	/**
	 * Add a correspondence of two resources affecting a certain aspect and thereby
	 * transitive implied correspondence. If the correspondence is already known or
	 * contradicts an existing incorrespondence, the correspondence will be discard
	 * silently.
	 * 
	 * @param resource1   first corresponding resource
	 * @param resource2   second corresponding resource
	 * @param aspect      aspect affected by the correspondence
	 * @param inputModel  model containing already known (in)correspondences
	 * @param outputModel model o write new (in)correspondences
	 */
	public static void addCorrespondence(Resource resource1, Resource resource2, Aspect aspect, Model inputModel,
			Model outputModel) {
		if (!existsOrContradicts(resource1, resource2, aspect, inputModel)) {
			add(resource1, resource2, aspect, false, inputModel, outputModel);
			addTransitiveInCorrespondence(resource1, resource2, aspect, false, inputModel, outputModel);
		}
	}

	/**
	 * Add an incorrespondence of two resources affecting a certain aspect and
	 * thereby transitive implied incorrespondence. If the incorrespondence is
	 * already known or contradicts an existing correspondence, the correspondence
	 * will be discard silently.
	 * 
	 * @param resource1   first corresponding resource
	 * @param resource2   second corresponding resource
	 * @param aspect      aspect affected by the correspondence
	 * @param inputModel  model containing already known (in)correspondences
	 * @param outputModel model o write new (in)correspondences
	 */
	public static void addIncorrespondence(Resource resource1, Resource resource2, Aspect aspect, Model inputModel,
			Model outputModel) {
		if (!existsOrContradicts(resource1, resource2, aspect, inputModel)) {
			add(resource1, resource2, aspect, true, inputModel, outputModel);
			addTransitiveInCorrespondence(resource1, resource2, aspect, true, inputModel, outputModel);
		}
	}

	private static SelectBuilder GET_CORRESPONDENCE_QUERY = new SelectBuilder().addVar(Var.alloc("resource1"))
			.addVar(Var.alloc("resource2")).addWhere(Var.alloc("set"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addWhere(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"));

	public static Stream<Resource[]> getCorrespondences(Aspect aspect, Model inputModel) {
		SelectBuilder queryBuilder = GET_CORRESPONDENCE_QUERY.clone();
		queryBuilder.setVar(Var.alloc("aspect"), aspect.name);
		ResultSet results = QueryExecutionFactory.create(queryBuilder.build(), inputModel).execSelect();
		return Stream.iterate(new Resource[] {}, e -> results.hasNext(), e -> {
			QuerySolution result = results.next();
			return new Resource[] { result.get("resource1").asResource(), result.get("resource2").asResource() };
		}).skip(1);// TODO check if skip 1 is correct
	}

	public static void addDeviation(Resource affectedResource, String affectedVariableName, Literal affectedValue,
			Resource comparedToDataset, Resource comparedToResource, Literal comparedToValue, Resource resource2,
			Aspect affectedAspect, Model outputAffectedDatasetMetaModel) {
		Resource qualityAnnotation = outputAffectedDatasetMetaModel.createResource(DQV.QualityAnnotation);
		Resource deviation = outputAffectedDatasetMetaModel.createResource(AV.QualityAnnotationBody);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, deviation);
		qualityAnnotation.addProperty(AV.affectedAspect, affectedAspect.name);
		qualityAnnotation.addLiteral(AV.affectedVariableName, affectedVariableName);
		qualityAnnotation.addLiteral(AV.affectedValue, affectedValue);
		qualityAnnotation.addProperty(AV.comparedToDataset, comparedToDataset);
		qualityAnnotation.addProperty(AV.comparedToResource, comparedToResource);
		qualityAnnotation.addLiteral(AV.comparedToValue, comparedToValue);
	}
}
