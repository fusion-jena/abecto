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
package de.uni_jena.cs.fusion.abecto.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.datatypes.xsd.impl.XSDDateType;
import org.apache.jena.datatypes.xsd.impl.XSDDouble;
import org.apache.jena.datatypes.xsd.impl.XSDFloat;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;

import com.github.jsonldjava.shaded.com.google.common.base.Objects;

import de.uni_jena.cs.fusion.abecto.Parameter;

public class LiteralValueComparisonProcessor extends AbstractValueComparisonProcessor<LiteralValueComparisonProcessor> {

	/**
	 * Language patterns to filter compared literals. If not empty, only string
	 * literals will be loaded, that match at least on of these patterns. String
	 * literals without language tag will match with "", all string literals with
	 * language tag match with "*". Default: empty
	 */
	@Parameter
	public Collection<String> languageFilterPatterns = new ArrayList<>();
	/**
	 * If true, a literal of the type xsd:date and a literal of the type
	 * xsd:dateTime with equal year, month and day part will match.
	 */
	@Parameter
	public boolean allowTimeSkip;
	/**
	 * If true, literals of the type xsd:string or xsd:dateTime with equal lexical
	 * value but different language tag will match.
	 */
	@Parameter
	public boolean allowLangTagSkip;

	@Override
	public boolean useValue(RDFNode value) {
		if (languageFilterPatterns.isEmpty()) {
			return true;
		} else {
			String langStr = value.asLiteral().getLanguage();
			return languageFilterPatterns.stream()
					.anyMatch(languageFilterPattern -> NodeFunctions.langMatches(langStr, languageFilterPattern));
		}
	}

	@Override
	public boolean isValidValue(RDFNode value) {
		return value.isLiteral();
	}

	@Override
	public String invalidValueComment() {
		return "Should be a literal.";
	}

	@Override
	public boolean equivalentValues(RDFNode value1, RDFNode value2) {
		if (!value1.isLiteral() || !value2.isLiteral()) {
			return false;
		}

		Literal literal1 = value1.asLiteral();
		Literal literal2 = value2.asLiteral();

		// same type/subtype check
		if (literal1.sameValueAs(literal2)) {
			return true;
		}

		RDFDatatype type1 = literal1.getDatatype();
		RDFDatatype type2 = literal2.getDatatype();

		// comparison of xsd:date and xsd:dateTime
		if (allowTimeSkip && (type1 instanceof XSDDateType && type2 instanceof XSDDateTimeType
				|| type1 instanceof XSDDateTimeType && type2 instanceof XSDDateType)) {
			XSDDateTime date1 = ((XSDDateTime) literal1.getValue());
			XSDDateTime date2 = ((XSDDateTime) literal2.getValue());
			return date1.getDays() == date2.getDays() //
					&& date1.getMonths() == date2.getMonths() //
					&& date1.getYears() == date2.getYears();
		}

		// ignore lang tags
		if (allowLangTagSkip && (type1 instanceof XSDBaseStringType || type1 instanceof RDFLangString)
				&& (type2 instanceof XSDBaseStringType || type2 instanceof RDFLangString)) {
			String string1 = literal1.getString();
			String string2 = literal2.getString();
			return Objects.equal(string1, string2);
		}

		// comparison of different number types
		try {
			BigDecimal decimal1, decimal2;

			// get precise BigDecimal of literal 1 and handle special cases of float/double
			if (type1 instanceof XSDBaseNumericType) {
				decimal1 = new BigDecimal(literal1.getLexicalForm());
			} else if (type1 instanceof XSDDouble) {
				double value1Double = literal1.getDouble();
				// handle special cases
				if (value1Double == Double.NaN) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.NaN;
				} else if (value1Double == Double.NEGATIVE_INFINITY) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.NEGATIVE_INFINITY;
				} else if (value1Double == Double.POSITIVE_INFINITY) {
					return type2 instanceof XSDFloat && literal2.getFloat() == Float.POSITIVE_INFINITY;
				}
				// get value as BigDecimal
				decimal1 = new BigDecimal(value1Double);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value1Double) or new
				 * BigDecimal(literal1.getLexicalForm()) to represented value from the double
				 * value space, not the double lexical space
				 */
			} else if (type1 instanceof XSDFloat) {
				float value1Float = literal1.getFloat();
				// handle special cases
				if (value1Float == Double.NaN) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.NaN;
				} else if (value1Float == Double.NEGATIVE_INFINITY) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.NEGATIVE_INFINITY;
				} else if (value1Float == Double.POSITIVE_INFINITY) {
					return type2 instanceof XSDDouble && literal2.getDouble() == Double.POSITIVE_INFINITY;
				}
				// get value as BigDecimal
				decimal1 = new BigDecimal(value1Float);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value1Float) or new
				 * BigDecimal(literal1.getLexicalForm()) to represented value from the float
				 * value space, not the float lexical space
				 */
			} else {
				return false;
			}

			// get precise BigDecimal of literal 2
			if (type2 instanceof XSDBaseNumericType) {
				decimal2 = new BigDecimal(literal2.getLexicalForm());
			} else if (type2 instanceof XSDDouble) {
				double value2Double = literal2.getDouble();
				// handle special cases
				if (value2Double == Double.NaN || value2Double == Double.NEGATIVE_INFINITY
						|| value2Double == Double.POSITIVE_INFINITY) {
					return false;
				}
				// get value as BigDecimal
				decimal2 = new BigDecimal(value2Double);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value2Double) or new
				 * BigDecimal(literal2.getLexicalForm()) to represented value from the double
				 * value space, not the double lexical space
				 */
			} else if (type2 instanceof XSDFloat) {
				float value2Float = literal2.getFloat();
				// handle special cases
				if (value2Float == Float.NaN || value2Float == Float.NEGATIVE_INFINITY
						|| value2Float == Float.POSITIVE_INFINITY) {
					return false;
				}
				// get value as BigDecimal
				decimal2 = new BigDecimal(value2Float);
				/*
				 * NOTE: don't use BigDecimal#valueOf(value2Float) or new
				 * BigDecimal(literal2.getLexicalForm()) to represented value from the float
				 * value space, not the float lexical space
				 */
			} else {
				return false;
			}

			// compare BigDecimals
			return decimal1.compareTo(decimal2) == 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
