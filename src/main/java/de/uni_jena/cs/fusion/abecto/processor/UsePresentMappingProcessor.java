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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
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
import de.uni_jena.cs.fusion.abecto.Vocabularies;
import de.uni_jena.cs.fusion.abecto.Parameter;

public class UsePresentMappingProcessor extends Processor {
	final static Logger log = LoggerFactory.getLogger(UsePresentMappingProcessor.class);

	@Parameter
	public Resource aspect;
	@Parameter
	public Collection<String> assignmentPaths;

	@Override
	public void run() {
		Aspect aspect = getAspects().get(this.aspect);
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

				// execute query for each dataset
				for (Resource dataset : this.getInputDatasets()) {
					Model inputPrimaryModel = this.getInputPrimaryModelUnion(dataset);
					Model metaModel = this.getMetaModelUnion(null);
					Model outputMetaModel = this.getOutputMetaModel(null);

					ResultSet resultSet = QueryExecutionFactory.create(query, inputPrimaryModel).execSelect();
					while (resultSet.hasNext()) {
						QuerySolution solution = resultSet.next();
						try {
							Resource resource1 = solution.getResource("s");
							Resource resource2 = solution.getResource("o");
							Correspondences.addCorrespondence(resource1, resource2, aspect.getIri(), metaModel, outputMetaModel);
						} catch (ClassCastException e) {
							// TODO add issue to outputMetaModel
							log.warn(String.format("UnexpectedValueType: Subject or object is not a resource: %s %s %s",
									solution.get("s"), assignmentPath, solution.get("o")));
						}
					}
				}
			} catch (ParseException e) {
				throw new IllegalStateException("Failed to parse assignment path.", e);
			}
		}
	}
}
