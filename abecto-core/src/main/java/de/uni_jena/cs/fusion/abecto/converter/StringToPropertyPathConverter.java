/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.converter;

import java.io.ByteArrayInputStream;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.lang.sparql_11.SPARQLParser11;
import org.apache.jena.sparql.path.Path;

import com.fasterxml.jackson.databind.util.StdConverter;

import de.uni_jena.cs.fusion.abecto.Vocabularies;

public class StringToPropertyPathConverter extends StdConverter<String,Path> {

	@Override
	public Path convert(String value) throws DatatypeFormatException {
		try {
			SPARQLParser11 parser = new SPARQLParser11(new ByteArrayInputStream(value.getBytes()));
			parser.setPrologue(Vocabularies.getDefaultPrologue());
			return parser.Path();
		} catch (ParseException e) {
			throw new DatatypeFormatException(
					String.format("Lexical \"%s\" is not a valid SPARQL Property Path.", value), e);
		}
	}

}
