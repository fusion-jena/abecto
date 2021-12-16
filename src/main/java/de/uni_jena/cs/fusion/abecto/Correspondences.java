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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.rulesys.FBRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.PathFactory;

import de.uni_jena.cs.fusion.abecto.util.Queries;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class Correspondences {
	private static final Var RESOURCE_1 = Var.alloc("resource1");
	private static final Var RESOURCE_2 = Var.alloc("resource2");
	private static final Var RESOURCE_3 = Var.alloc("resource3");
	private static ExprFactory exprFactory = new ExprFactory();

	private final static List<Rule> correspondenceRules = Arrays.asList(
			// corresponds inverse
			Rule.parseRule("[ (?A " + AV.correspondsToResource + " ?B) "//
					+ "-> (?B " + AV.correspondsToResource + " ?A) ]"),
			// corresponds transitivity
			Rule.parseRule("[ (?A " + AV.correspondsToResource + " ?B),(?B " + AV.correspondsToResource + " ?C) "//
					+ "-> (?A " + AV.correspondsToResource + " ?C) ]"),
			// correspondsNot inverse
			Rule.parseRule("[ (?A " + AV.correspondsNotToResource + " ?B) "//
					+ "-> (?B " + AV.correspondsNotToResource + " ?A) ]"),
			// correspondsNot-corresponds chains
			Rule.parseRule("[ (?A " + AV.correspondsNotToResource + " ?B),(?B " + AV.correspondsToResource + " ?C) "//
					+ "-> (?A " + AV.correspondsNotToResource + " ?C) ]"));

	private static Model getTransitiveCorrespondencesModel(Model correspondencesModel) {
		return ModelFactory.createInfModel(new FBRuleReasoner(correspondenceRules), correspondencesModel);
	}

	/**
	 * Checks if a correspondence or incorrespondence for two given resources in a
	 * given aspect already exist.
	 * <p>
	 * <strong>Note:<strong> The use of this method is not mandatory, as it will
	 * also be called by {@link #addCorrespondence(Resource, Resource, Aspect)} and
	 * {@link #addIncorrespondence(Resource, Resource, Aspect)}.
	 * 
	 * @param resource1  first resource to check
	 * @param resource2  second resource to check
	 * @param inputModel model containing already known (in)correspondences
	 * @return {@code true}, if an equivalent or contradicting (in)correspondence
	 *         exists, otherwise {@code false}
	 */
	public static boolean correspondentOrIncorrespondent(Resource resource1, Resource resource2, Model inputModel) {
		Query query = new AskBuilder().addWhere(resource1, AV.correspondsToResource, resource2)
				.addUnion(new WhereBuilder().addWhere(resource1, AV.correspondsNotToResource, resource2)).build();
		return resource1.equals(resource2) || QueryExecutionFactory.create(query, inputModel).execAsk();
	}

	/**
	 * Checks if a correspondence for two given resources in a given aspect exists.
	 * 
	 * @param resource1  first resource to check
	 * @param resource2  second resource to check
	 * @param aspect1    aspect affected by the correspondence
	 * @param inputModel model containing already known correspondences
	 * @return {@code true}, if resources correspond to each, otherwise
	 *         {@code false}
	 */
	public static boolean correspond(Resource resource1, Resource resource2, Model inputModel) {
		inputModel = getTransitiveCorrespondencesModel(inputModel);
		Query query = new AskBuilder().addWhere(resource1, AV.correspondsToResource, resource2).build();
		return resource1.equals(resource2) || QueryExecutionFactory.create(query, inputModel).execAsk();
	}

	/**
	 * Checks if all given resources correspond to each other.
	 * 
	 * @param inputModel the model containing the correspondence data
	 * @param resources  the resources to check
	 * @return {@code true}, if all resources correspond to each other, otherwise
	 *         {@code false}
	 */
	public static boolean allCorrespondend(Model inputModel, Resource... resources) {
		if (resources.length < 2) {
			return true;
		}
		inputModel = getTransitiveCorrespondencesModel(inputModel);
		Query query = new AskBuilder()//
				// TODO replace workaround when a better solution exists
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_1).addWhereValueVar(RESOURCE_1, (Object[]) resources))
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_2).addWhereValueVar(RESOURCE_2, (Object[]) resources))
				.addFilter(exprFactory.ne(RESOURCE_1, RESOURCE_2))
				.addFilter(exprFactory
						.notexists(new WhereBuilder().addWhere(RESOURCE_1, AV.correspondsToResource, RESOURCE_2)))
				.build();
		return !QueryExecutionFactory.create(query, inputModel).execAsk();
	}

	/**
	 * Checks if any pair of the given resources is known to be incorrespondent.
	 * 
	 * @param inputModel the model containing the correspondence data
	 * @param resources  the resources to check
	 * @return {@code true}, if all resources are not incorrespondent to each other,
	 *         otherwise {@code false}
	 */
	public static boolean anyIncorrespondend(Model inputModel, Resource... resources) {
		if (resources.length < 2) {
			return false;
		}
		inputModel = getTransitiveCorrespondencesModel(inputModel);
		Query query = new AskBuilder()//
				// TODO replace workaround when a better solution exists
				// NOTE: addWhereValueVar() does not cross-join / add multiple VALUES clauses
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_1).addWhereValueVar(RESOURCE_1, (Object[]) resources))
				.addSubQuery(new SelectBuilder().addVar(RESOURCE_2).addWhereValueVar(RESOURCE_2, (Object[]) resources))
				.addFilter(exprFactory.ne(RESOURCE_1, RESOURCE_2))
				.addWhere(RESOURCE_1, AV.correspondsNotToResource, RESOURCE_2).build();
		return QueryExecutionFactory.create(query, inputModel).execAsk();
	}

	/**
	 * Adds correspondences of several resources affecting a certain aspect and
	 * thereby transitive implied correspondence. If the correspondences are already
	 * known or contradict an existing incorrespondence, the correspondences will be
	 * discard silently.
	 * 
	 * @param resources   the corresponding resources
	 * @param aspect      aspect affected by the correspondence
	 * @param inputModel  model containing already known (in)correspondences
	 * @param outputModel model to write new correspondences
	 */
	public static void addCorrespondence(Model inputModel, Model outputModel, Resource aspect,
			Collection<Resource> resources) {
		addCorrespondence(inputModel, outputModel, aspect, resources.toArray(l -> new Resource[l]));
	}

	/**
	 * Add correspondences of several resources belonging to a certain aspect. If
	 * all correspondences are already known or any correspondence contradict an
	 * existing incorrespondence, all correspondences will be discard silently.
	 * 
	 * @param resources   the corresponding resources
	 * @param aspect      aspect the corresponding resources belong to
	 * @param inputModel  model containing already known (in)correspondences
	 * @param outputModel model to write new correspondences
	 */
	public static void addCorrespondence(Model inputModel, Model outputModel, Resource aspect, Resource... resources) {
		if (resources.length < 2) {
			return;
		}
		if (!anyIncorrespondend(inputModel, resources) && !allCorrespondend(inputModel, resources)) {
			Query query = new ConstructBuilder()//
					.addConstruct(aspect, AV.relevantResource, resources[0])//
					.addConstruct(aspect, AV.relevantResource, RESOURCE_2)//
					.addConstruct(resources[0], AV.correspondsToResource, RESOURCE_2)//
					.addWhereValueVar(RESOURCE_2, (Object[]) Arrays.copyOfRange(resources, 1, resources.length))
					.build();
			QueryExecutionFactory.create(query, inputModel).execConstruct(outputModel);
		}
	}

	/**
	 * Adds an incorrespondence of two resources affecting a certain aspect. If the
	 * incorrespondence is already known or contradicts an existing correspondence,
	 * the correspondence will be discard silently.
	 * 
	 * @param inputModel  model containing already known (in)correspondences
	 * @param outputModel model to write new incorrespondences
	 * @param aspect      aspect affected by the correspondence
	 * @param resource1   first corresponding resource
	 * @param resource2   second corresponding resource
	 */
	public static void addIncorrespondence(Model inputModel, Model outputModel, Resource aspect, Resource resource1,
			Resource resource2) {
		if (!correspondentOrIncorrespondent(resource1, resource2, inputModel)) {
			Query query = new ConstructBuilder()//
					.addConstruct(aspect, AV.relevantResource, resource1)//
					.addConstruct(aspect, AV.relevantResource, resource2)//
					.addConstruct(resource1, AV.correspondsNotToResource, resource2)//
					.build();
			QueryExecutionFactory.create(query, inputModel).execConstruct(outputModel);
		}
	}

	/**
	 * Returns {@link List Lists} of {@link Resource Resources} that belong to a
	 * given aspect and correspond to each other.
	 * 
	 * @param inputModel the source of the correspondence data
	 * @param aspect     the aspect all returned {@link Resource Resources} must
	 *                   belong to
	 * @return the {@link List Lists} of corresponding {@link Resource Resources}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Stream<List<Resource>> getCorrespondenceSets(Model inputModel, Resource aspect) {
		inputModel = getTransitiveCorrespondencesModel(inputModel);
		Query query = new SelectBuilder().addVar(RESOURCE_1).addVar(RESOURCE_2)
				.addWhere(aspect, AV.relevantResource, RESOURCE_1)//
				.addWhere(aspect, AV.relevantResource, RESOURCE_2)//
				.addWhere(new TriplePath(RESOURCE_1,
						PathFactory.pathZeroOrOne(PathFactory.pathLink(AV.correspondsToResource.asNode())), RESOURCE_2))
				.addFilter(exprFactory
						.notexists(new WhereBuilder().addWhere(RESOURCE_1, AV.correspondsToResource, RESOURCE_3)
								.addFilter(exprFactory.gt(exprFactory.str(RESOURCE_1), exprFactory.str(RESOURCE_3)))))
				.addOrderBy(RESOURCE_1).build();
		return Queries.getStreamOfResultsGroupedBy(inputModel, query, RESOURCE_1.getName())
				.map(m -> (List<Resource>) (List) m.get(RESOURCE_2.getName()));
	}
}
