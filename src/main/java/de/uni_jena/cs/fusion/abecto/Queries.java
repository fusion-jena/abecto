package de.uni_jena.cs.fusion.abecto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.jena.atlas.lib.StreamOps;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class Queries {

	/**
	 * Executes a SELECT {@link Query} on a {@link Model} and returns a finite,
	 * unordered {@link Stream} of the first result column. {@code null} values will
	 * be omitted.
	 * 
	 * @param query select query to execute
	 * @param model target of the query
	 * @return finite, unordered {@link Stream} of the first column
	 */
	public static Stream<RDFNode> getStreamOfFirstResultColumn(Model model, Query query) {
		ResultSet results = QueryExecutionFactory.create(query, model).execSelect();
		String varName = results.getResultVars().get(0);
		return StreamOps.stream(results).map(result -> result.get(varName));
	}

	/**
	 * Executes a SELECT {@link Query} on a {@link Model} and returns a finite,
	 * unordered {@link Stream} of the first result column of {@link Resource
	 * Resources}.
	 * 
	 * @param query select query to execute
	 * @param model target of the query
	 * @return finite, unordered {@link Stream} of the first column of
	 *         {@link Resource Resources}
	 */
	public static Stream<Resource> getStreamOfFirstResultColumnAsResource(Model model, Query query) {
		return getStreamOfFirstResultColumn(model, query).map(RDFNode::asResource);
	}

	/**
	 * Executes a SELECT {@link Query} on a {@link Model} and returns a finite,
	 * unordered {@link Stream} of the first result column of {@link Literal
	 * Literals}.
	 * 
	 * @param query select query to execute
	 * @param model target of the query
	 * @return finite, unordered {@link Stream} of the first column of
	 *         {@link Literal Literals}
	 */
	public static Stream<Literal> getStreamOfFirstResultColumnAsLiteral(Model model, Query query) {
		return getStreamOfFirstResultColumn(model, query).map(RDFNode::asLiteral);
	}

	/**
	 * Executes a SELECT {@link Query} on a {@link Model} and returns a finite,
	 * unordered {@link Stream} of the {@link QuerySolution QuerySolutions}
	 * 
	 * @param query select query to execute
	 * @param model target of the query
	 * @return finite, unordered {@link Stream} of the {@link QuerySolution
	 *         QuerySolutions}
	 */
	public static Stream<QuerySolution> getStreamOfResults(Model model, Query query) {
		ResultSet results = QueryExecutionFactory.create(query, model).execSelect();
		return StreamOps.stream(results);
	}

	private static class GroupedResultSupplier implements Supplier<Map<String, List<RDFNode>>> {
		QuerySolution next;
		ResultSet results;
		String groupBy;
		List<String> vars;

		public GroupedResultSupplier(ResultSet results, String groupBy) {
			if (results.hasNext()) {
				this.results = results;
				this.groupBy = groupBy;
				this.next = results.next();
				this.vars = results.getResultVars();
				this.vars.remove(groupBy);
			}
		}

		@Override
		public Map<String, List<RDFNode>> get() {
			if (this.next != null) {
				Map<String, List<RDFNode>> result = new HashMap<>();
				this.vars.forEach(k -> result.put(k, new ArrayList<>()));
				Resource groupByValue = next.getResource(this.groupBy);
				result.put(this.groupBy, Collections.singletonList(groupByValue));
				do {
					for (String var : this.vars) {
						result.get(var).add(next.get(var));
					}
					if (this.results.hasNext()) {
						this.next = this.results.next();
					} else {
						this.next = null;
					}
				} while (this.next != null && next.getResource(this.groupBy).equals(groupByValue));
				return result;
			} else {
				return null;
			}
		}
	}

	/**
	 * Executes a SELECT {@link Query} on a {@link Model} and returns a finite,
	 * unordered {@link Stream} of the {@link QuerySolution QuerySolutions}
	 * 
	 * @param query select query to execute
	 * @param model target of the query
	 * @return finite, unordered {@link Stream} of the {@link QuerySolution
	 *         QuerySolutions}
	 */
	public static Stream<Map<String, List<RDFNode>>> getStreamOfResultsGroupedBy(Model model, Query query,
			String groupBy) {
		ResultSet results = QueryExecutionFactory.create(query, model).execSelect();
		return Stream.generate(new GroupedResultSupplier(results, groupBy)).takeWhile(Objects::nonNull);
	}
}
