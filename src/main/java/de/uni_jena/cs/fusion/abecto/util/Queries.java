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
		QuerySolution current;
		ResultSet results;
		String groupByKey;
		List<String> vars;

		public GroupedResultSupplier(ResultSet results, String groupByKey) {
			if (results.hasNext()) {
				this.results = results;
				this.groupByKey = groupByKey;
				this.current = nextOrNull();
				this.vars = results.getResultVars();
				this.vars.remove(groupByKey);
			}
		}

		@Override
		public Map<String, List<RDFNode>> get() {
			if (this.current != null) {
				Map<String, List<RDFNode>> group = new HashMap<>();
				this.vars.forEach(k -> group.put(k, new ArrayList<>()));
				Resource currentGroupByKey = current.getResource(this.groupByKey);
				group.put(this.groupByKey, Collections.singletonList(currentGroupByKey));
				do {
					for (String var : this.vars) {
						group.get(var).add(current.get(var));
					}
					this.current = nextOrNull();
				} while (this.current != null && current.getResource(this.groupByKey).equals(currentGroupByKey));
				return group;
			} else {
				return null;
			}
		}

		private QuerySolution nextOrNull() {
			if (this.results.hasNext()) {
				return this.results.next();
			}
			return null;
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
