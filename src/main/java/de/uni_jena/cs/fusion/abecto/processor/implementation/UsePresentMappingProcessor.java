package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor.filterMappings;
import static de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor.getKnownMappings;
import static de.uni_jena.cs.fusion.abecto.processor.AbstractMappingProcessor.saveMappings;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;
import de.uni_jena.cs.fusion.abecto.processor.AbstractMetaProcessor;
import de.uni_jena.cs.fusion.abecto.processor.MappingProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.util.Default;

public class UsePresentMappingProcessor extends AbstractMetaProcessor<UsePresentMappingProcessor.Parameter>
		implements MappingProcessor<UsePresentMappingProcessor.Parameter> {

	@JsonSerialize
	public static class Parameter implements ParameterModel {
		public Collection<String> assignmentPaths = new HashSet<>();
	}

	@Override
	public final void computeResultModel() throws Exception {

		Collection<Mapping> newMappings = new HashSet<>();
		for (String unparsedAssignmentPath : this.getParameters().assignmentPaths) {
			try {
				// get path
				SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(unparsedAssignmentPath.getBytes()));
				parser.setPrologue(Default.PROLOGUE);
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

				// execute query for each ontology
				for (Entry<UUID, Model> modelGroupOfOntology : this.inputGroupModels.entrySet()) {
					ResultSet resultSet = QueryExecutionFactory.create(query, modelGroupOfOntology.getValue())
							.execSelect();
					while (resultSet.hasNext()) {
						QuerySolution solution = resultSet.next();
						try {
							Resource resource1 = solution.getResource("s");
							Resource resource2 = solution.getResource("o");
							newMappings.add(Mapping.of(resource1, resource2));
						} catch (ClassCastException e) {
							Issue issue = new Issue(null, modelGroupOfOntology.getKey(), null, "UnexpectedValueType",
									String.format("Subject or object is not a resource: %s %s %s", solution.get("s"),
											assignmentPath, solution.get("o")));
							SparqlEntityManager.insert(issue, this.getResultModel());
						}
					}
				}
			} catch (ParseException e) {
				throw new IllegalStateException("Failed to parse assignment path.", e);
			}
		}

		Collection<Mapping> knownMappings = getKnownMappings(this.metaModel);
		Collection<Mapping> acceptedMappings = filterMappings(newMappings, knownMappings);
		saveMappings(acceptedMappings, this.getResultModel());
	}
}
