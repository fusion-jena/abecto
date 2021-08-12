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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.OA;

public class Metadata {

	private static final Var setVar = Var.alloc("set");
	private static final Var setTypeVar = Var.alloc("setType");
	private static final Var resourceVar = Var.alloc("resource");
	private static final Var resource1Var = Var.alloc("resource1");
	private static final Var resource2Var = Var.alloc("resource2");
	private static final Var aspectVar = Var.alloc("aspect");

	private static AskBuilder CORRESPONDENCE_EXISTS_OR_CONTRADICTS_QUERY = new AskBuilder()
			.addWhere(setVar, RDF.type, setTypeVar).addWhere(setVar, AV.containedResource, resource1Var)
			.addWhere(setVar, AV.containedResource, resource2Var).addWhere(setVar, AV.affectedAspect, aspectVar);

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
		queryBuilder.setVar(resource1Var, resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(aspectVar, aspect);
		return QueryExecutionFactory.create(queryBuilder.build(), inputModel).execAsk();
	}

	private static Query CORRESPONDE_QUERY = new AskBuilder().addWhere(setVar, RDF.type, AV.CorrespondenceSet)
			.addWhere(setVar, AV.containedResource, resource1Var).addWhere(setVar, AV.containedResource, resource2Var)
			.addWhere(setVar, AV.affectedAspect, aspectVar).build();

	/**
	 * Check if a correspondence for two given resources in a given aspect exist.
	 * 
	 * @param resource1  first resource to check
	 * @param resource2  second resource to check
	 * @param aspect     aspect affected by the correspondence
	 * @param inputModel model containing already known correspondences
	 * @return {@code true}, if resources correspond to each, otherwise
	 *         {@code false}
	 */
	public static boolean correspond(Resource resource1, Resource resource2, Resource aspect, Model inputModel) {
		Query query = AskBuilder.rewrite(CORRESPONDE_QUERY.cloneQuery(),
				Map.of(aspectVar, aspect.asNode(), resource1Var, resource1.asNode(), resource2Var, resource2.asNode()));
		return QueryExecutionFactory.create(query, inputModel).execAsk();
	}

	private static ConstructBuilder ADD_CORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(setVar, RDF.type, setTypeVar).addConstruct(setVar, AV.containedResource, resource1Var)
			.addConstruct(setVar, AV.containedResource, resource2Var)
			.addConstruct(setVar, AV.affectedAspect, aspectVar);

	private static void add(Resource resource1, Resource resource2, Aspect aspect, boolean incorrespondence,
			Model inputModel, Model outputModel) {
		ConstructBuilder queryBuilder = ADD_CORRESPONDENCE_QUERY.clone();
		queryBuilder.setVar(setTypeVar, incorrespondence ? AV.IncorrespondenceSet : AV.CorrespondenceSet);
		queryBuilder.setVar(resource1Var, resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(aspectVar, aspect);
		QueryExecutionFactory.create(queryBuilder.build(), outputModel).execConstruct(outputModel);
	}

	private static ConstructBuilder ADD_TRANSITIVE_CORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(setVar, AV.containedResource, Var.alloc("resourceTransitive"))
			.addWhere(setVar, RDF.type, AV.CorrespondenceSet).addWhere(setVar, AV.containedResource, resource1Var)
			.addWhere(setVar, AV.containedResource, resource2Var).addWhere(setVar, AV.affectedAspect, aspectVar)
			.addWhere(Var.alloc("otherSet"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("otherSet"), AV.containedResource, Var.alloc("resourceTransitive"))
			.addWhere(Var.alloc("otherSet"), AV.affectedAspect, aspectVar)//
			.addWhere(new WhereBuilder()//
					.addWhere(Var.alloc("otherSet"), AV.containedResource, resource1Var).addUnion(new WhereBuilder()//
							.addWhere(Var.alloc("otherSet"), AV.containedResource, resource2Var)));

	private static ConstructBuilder ADD_TRANSITIVE_INCORRESPONDENCE_QUERY = new ConstructBuilder()
			.addConstruct(Var.alloc("transitiveSet"), RDF.type, AV.IncorrespondenceSet)
			.addConstruct(Var.alloc("transitiveSet"), AV.containedResource, Var.alloc("resourceTransitive1"))
			.addConstruct(Var.alloc("transitiveSet"), AV.containedResource, Var.alloc("resourceTransitive2"))
			.addConstruct(Var.alloc("transitiveSet"), AV.affectedAspect, aspectVar)
			.addWhere(Var.alloc("otherSet"), RDF.type, AV.CorrespondenceSet)
			.addWhere(Var.alloc("otherSet"), AV.containedResource, Var.alloc("resourceTransitive2"))
			.addWhere(Var.alloc("otherSet"), AV.affectedAspect, aspectVar)//
			.addWhere(new WhereBuilder()//
					.addWhere(Var.alloc("otherSet"), AV.containedResource, resource1Var)
					.addBind(new ExprVar(Var.alloc("resource2")), Var.alloc("resourceTransitive1"))
					.addUnion(new WhereBuilder()//
							.addWhere(Var.alloc("otherSet"), AV.containedResource, resource2Var)
							.addBind(new ExprVar(resource1Var), Var.alloc("resourceTransitive1"))));

	private static void addTransitiveInCorrespondence(Resource resource1, Resource resource2, Aspect aspect,
			boolean incorrespondence, Model inputModel, Model outputModel) {
		ConstructBuilder queryBuilder = (incorrespondence ? ADD_TRANSITIVE_INCORRESPONDENCE_QUERY
				: ADD_TRANSITIVE_CORRESPONDENCE_QUERY).clone();
		queryBuilder.setVar(resource1Var, resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(aspectVar, aspect);
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

	private static Query GET_CORRESPONDENCE_SETS_QUERY = new SelectBuilder().addVar(setVar).addVar(resourceVar)
			.addWhere(setVar, RDF.type, AV.CorrespondenceSet).addWhere(setVar, AV.containedResource, resourceVar)
			.addWhere(setVar, AV.affectedAspect, aspectVar).addOrderBy(setVar).build();

	private static class CorrespondencesSupplier implements Supplier<List<Resource>> {
		QuerySolution next;
		ResultSet solutions;
		String set = Metadata.setVar.getVarName();
		String resource = Metadata.resourceVar.getVarName();

		public CorrespondencesSupplier(ResultSet results) {
			if (results.hasNext()) {
				this.solutions = results;
				this.next = results.next();
			}
		}

		@Override
		public List<Resource> get() {
			if (next != null) {
				List<Resource> results = new ArrayList<>();
				Resource setValue = next.getResource(set);
				do {
					results.add(next.getResource(resource));
					if (solutions.hasNext()) {
						next = solutions.next();
					} else {
						next = null;
					}
				} while (next != null && next.getResource(set).equals(setValue));
				return results;
			} else {
				return null;
			}
		}
	}

	public static Stream<List<Resource>> getCorrespondenceSets(Aspect aspect, Model inputModel) {
		Query query = SelectBuilder.rewrite(GET_CORRESPONDENCE_SETS_QUERY.cloneQuery(),
				Collections.singletonMap(aspectVar, aspect.name.asNode()));
		ResultSet results = QueryExecutionFactory.create(query, inputModel).execSelect();
		return Stream.generate(new CorrespondencesSupplier(results)).takeWhile(Objects::nonNull);
	}

	public static void addDeviation(Resource affectedResource, String affectedVariableName, RDFNode affectedValue,
			Resource comparedToDataset, Resource comparedToResource, RDFNode comparedToValue, Aspect affectedAspect,
			Model outputAffectedDatasetMetaModel) {
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
