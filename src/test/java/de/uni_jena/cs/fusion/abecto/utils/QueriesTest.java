package de.uni_jena.cs.fusion.abecto.utils;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.util.Queries;

public class QueriesTest {

	@Test
	public void x() {
		String category = "entity";
		String pattern = "?entity <http://www.w3.org/2000/01/rdf-schema#rdfs:label> ?label .";

		Model m = Models.getEmptyOntModel();
		Query metaConstructQuery = Queries.patternConstruct().addValueRow(category, pattern).build();
		QueryExecutionFactory.create(metaConstructQuery, m).execConstruct(m);

		Query select = Queries.patternSelect(NodeFactory.createLiteral(category)).build();
		ResultSet result = QueryExecutionFactory.create(select, m).execSelect();
		Assertions.assertEquals(pattern, result.next().getLiteral("pattern").getString());
	}
}
