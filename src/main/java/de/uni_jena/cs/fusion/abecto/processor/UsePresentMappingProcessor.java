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

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceRequiredException;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Aspect;
import de.uni_jena.cs.fusion.abecto.Correspondences;
import de.uni_jena.cs.fusion.abecto.Metadata;
import de.uni_jena.cs.fusion.abecto.Parameter;
import de.uni_jena.cs.fusion.abecto.Vocabularies;

public class UsePresentMappingProcessor extends Processor<UsePresentMappingProcessor> {
	final static Logger log = LoggerFactory.getLogger(UsePresentMappingProcessor.class);

	@Parameter
	public Resource aspect;
	@Parameter
	public Collection<String> assignmentPaths;

	@Override
	public void run() {
		Aspect aspect = Objects.requireNonNull(getAspects().get(this.aspect), "Unknown aspect.");
		for (String unparsedAssignmentPath : this.assignmentPaths) {
			try {
				// get path
				SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(unparsedAssignmentPath.getBytes()));
				parser.setPrologue(Vocabularies.getDefaultPrologue());
				Path assignmentPath = parser.Path();

				// create query
				Query query = new Query();
				query.setQuerySelectType();
				Var subject = Var.alloc("s");
				Var object = Var.alloc("o");
				query.addResultVar(subject);
				query.addResultVar(object);
				ElementPathBlock block = new ElementPathBlock();
				block.addTriple(new TriplePath(subject, assignmentPath, object));
				query.setQueryPattern(block);

				Model metaModel = this.getMetaModelUnion(null);
				Model outputMetaModel = this.getOutputMetaModel(null);
				// execute query for each dataset
				for (Resource dataset : this.getDatasets()) {
					Model inputPrimaryModel = this.getInputPrimaryModelUnion(dataset);

					// get aspects resource
					Set<Resource> aspectResources = Aspect.getResourceKeys(aspect, dataset, inputPrimaryModel);

					ResultSet resultSet = QueryExecutionFactory.create(query, inputPrimaryModel).execSelect();
					while (resultSet.hasNext()) {
						QuerySolution solution = resultSet.next();
						RDFNode node1 = solution.get("s");
						RDFNode node2 = solution.get("o");
						if (node1.isResource() && aspectResources.contains(node1.asResource())) {
							try {
								Correspondences.addCorrespondence(metaModel, outputMetaModel, this.aspect,
										node1.asResource(), node2.asResource());
							} catch (ResourceRequiredException e) {
								Metadata.addIssue(node1.asResource(), null, node2, this.aspect, "Invalid Value",
										String.format(
												"Failed to get corresponding resource, found a literal: <%s> %s \"%s\"^^<%s>",
												node1, assignmentPath, node2, node2.asLiteral().getDatatypeURI()),
										outputMetaModel);
							}
						} else if (node2.isResource() && aspectResources.contains(node2.asResource())) {
							try {
								Correspondences.addCorrespondence(metaModel, outputMetaModel, this.aspect,
										node1.asResource(), node2.asResource());
							} catch (ResourceRequiredException e) {
								Metadata.addIssue(node2.asResource(), null, node1, this.aspect, "Invalid Value",
										String.format(
												"Failed to get corresponding resource, found a literal: \"%s\"^^<%s> <%s> %s",
												node1, node1.asLiteral().getDatatypeURI(), assignmentPath, node2),
										outputMetaModel);
							}
						}
						// else ignore
					}
				}
			} catch (ParseException e) {
				throw new IllegalStateException("Failed to parse assignment path.", e);
			}
		}
	}
}
