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
package de.uni_jena.cs.fusion.abecto.datatype;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class SparqlQueryType extends BaseDatatype {

	public SparqlQueryType() {
		super(AV.SparqlQuery.getURI());
	}

	@Override
	public String unparse(Object value) {
		if (value instanceof Query) {
			return ((Query) value).toString();
		}
		throw new IllegalArgumentException(String.format("Value is not a %s.", Query.class.getCanonicalName()));
	}

	@Override
	public Query parse(String lexicalForm) throws DatatypeFormatException {
		try {
			return QueryFactory.create(lexicalForm, Syntax.syntaxSPARQL);
		} catch (QueryException e) {
			throw new DatatypeFormatException("Not a valid SPARQL query.", e);
		}
	}

	@Override
	public boolean isValidLiteral(LiteralLabel lit) {
		return equals(lit.getDatatype()) && isValidValue(lit.getValue());
	}

	@Override
	public boolean isValidValue(Object valueForm) {
		return valueForm instanceof Query;
	}

	@Override
	public Class<?> getJavaClass() {
		return Query.class;
	}

}
