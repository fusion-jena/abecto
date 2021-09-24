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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.vocabulary.XSD;

public class XsdDateTimeStampType extends BaseDatatype {

	public XsdDateTimeStampType() {
		super(XSD.dateTimeStamp.getURI());
	}

	@Override
	public String unparse(Object value) {
		if (value instanceof OffsetDateTime) {
			return ((OffsetDateTime) value).toString();
		}
		throw new IllegalArgumentException(
				String.format("Value is not a %s.", OffsetDateTime.class.getCanonicalName()));
	}

	@Override
	public OffsetDateTime parse(String lexicalForm) throws DatatypeFormatException {
		try {
			return OffsetDateTime.parse(lexicalForm);
		} catch (DateTimeParseException e) {
			throw new DatatypeFormatException("Invalid xsd:dateTimeStamp.", e);
		}
	}

	@Override
	public boolean isValidLiteral(LiteralLabel lit) {
		return this.equals(lit.getDatatype()) && isValidValue(lit.getValue());
	}

	@Override
	public boolean isValidValue(Object valueForm) {
		return valueForm instanceof OffsetDateTime;
	}

	@Override
	public Class<?> getJavaClass() {
		return OffsetDateTime.class;
	}

}
