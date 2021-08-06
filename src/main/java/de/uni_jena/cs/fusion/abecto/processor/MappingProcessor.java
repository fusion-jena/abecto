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
package de.uni_jena.cs.fusion.abecto.processor;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.vocabulary.RDF;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public abstract class MappingProcessor extends Processor {
	private final AskBuilder CONTRADICTION_QUERY_BUILDER = new AskBuilder()
			.addWhere(Var.alloc("set"), RDF.type, Var.alloc("setType"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addWhere(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addWhere(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"));

	public final boolean existsOrContradicts(Resource resource1, Resource resource2, Aspect aspect) {
		AskBuilder queryBuilder = CONTRADICTION_QUERY_BUILDER.clone();
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		return QueryExecutionFactory.create(queryBuilder.build(), this.getMetaModelUnion(null)).execAsk();
	}

	private final ConstructBuilder ADD_QUERY = new ConstructBuilder()
			.addConstruct(Var.alloc("set"), RDF.type, Var.alloc("setType"))
			.addConstruct(Var.alloc("set"), AV.containdResource, Var.alloc("resource1"))
			.addConstruct(Var.alloc("set"), AV.containdResource, Var.alloc("resource2"))
			.addConstruct(Var.alloc("set"), AV.affectedAspect, Var.alloc("aspect"));

	private final void add(Resource resource1, Resource resource2, Aspect aspect, boolean incorrespondence) {
		ConstructBuilder queryBuilder = ADD_QUERY.clone();
		queryBuilder.setVar(Var.alloc("setType"), incorrespondence ? AV.IncorrespondenceSet : AV.CorrespondenceSet);
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		QueryExecutionFactory.create(queryBuilder.build(), this.getOutputMetaModel(null))
				.execConstruct(this.getOutputMetaModel(null));
	}

	private final ConstructBuilder ADD_TRANSITIVE_CORRESPONDENCE_QUERY = new ConstructBuilder()
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

	private final ConstructBuilder ADD_TRANSITIVE_INCORRESPONDENCE_QUERY = new ConstructBuilder()
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

	private final void addTransitive(Resource resource1, Resource resource2, Aspect aspect, boolean incorrespondence) {
		ConstructBuilder queryBuilder = (incorrespondence ? ADD_TRANSITIVE_INCORRESPONDENCE_QUERY
				: ADD_TRANSITIVE_CORRESPONDENCE_QUERY).clone();
		queryBuilder.setVar(Var.alloc("resource1"), resource1);
		queryBuilder.setVar(Var.alloc("resource2"), resource2);
		queryBuilder.setVar(Var.alloc("aspect"), aspect);
		QueryExecutionFactory.create(queryBuilder.build(), this.getMetaModelUnion(null))
				.execConstruct(this.getOutputMetaModel(null));
	}

	public final void addCorrespondence(Resource resource1, Resource resource2, Aspect aspect) {
		if (!this.existsOrContradicts(resource1, resource2, aspect)) {
			this.add(resource1, resource2, aspect, false);
			this.addTransitive(resource1, resource2, aspect, false);
		}
	}

	public final void addIncorrespondence(Resource resource1, Resource resource2, Aspect aspect) {
		if (!this.existsOrContradicts(resource1, resource2, aspect)) {
			this.add(resource1, resource2, aspect, true);
			this.addTransitive(resource1, resource2, aspect, true);
		}
	}

	/**
	 * TODO DOCU Computes the mappings of two models. The mappings may contain
	 * category meta data.
	 * 
	 * @param model1      the first model to process
	 * @param model2      the second model to process
	 * @param ontologyId1 the ontology id of the first model
	 * @param ontologyId2 the ontology id of the second model
	 * @return the computed mappings
	 */
	public abstract void mapDatasets(Resource dataset1, Resource dataset2);

	@Override
	public final void run() {
		for (Resource dataset1 : this.getInputDatasets()) {
			for (Resource dataset2 : this.getInputDatasets()) {
				if (dataset1.getURI().compareTo(dataset2.getURI()) > 0) {
					this.mapDatasets(dataset1, dataset2);
				}
			}
		}
	}
}
