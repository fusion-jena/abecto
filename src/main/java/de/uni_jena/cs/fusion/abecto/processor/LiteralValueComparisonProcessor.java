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

import java.math.BigDecimal;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.datatypes.xsd.impl.XSDDouble;
import org.apache.jena.datatypes.xsd.impl.XSDFloat;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

public class LiteralValueComparisonProcessor extends AbstractValueComparisonProcessor<LiteralValueComparisonProcessor> {

	@Override
	public boolean isValidValue(RDFNode value) {
		if (!value.isLiteral()) {
			return false;
		}
		return true;
	}

	@Override
	public String invalidValueComment() {
		return "Should be a literal.";
	}

	@Override
	public boolean equivalentValues(RDFNode value1, RDFNode value2) {
		Literal literal1 = value1.asLiteral();
		Literal literal2 = value2.asLiteral();

		// same type/subtype check
		if (literal1.sameValueAs(literal2)) {
			return true;
		}

		RDFDatatype type1 = literal1.getDatatype();
		RDFDatatype type2 = literal2.getDatatype();

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
