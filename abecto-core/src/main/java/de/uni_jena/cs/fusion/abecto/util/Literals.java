/*-
 * Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
 *                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 * Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
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
-*/

package de.uni_jena.cs.fusion.abecto.util;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.*;
import org.apache.jena.rdf.model.Literal;

import java.math.BigDecimal;
import java.util.Objects;

public class Literals {

    public static boolean equivalentLiteralsOfSameTypes(Literal literal1, Literal literal2) {
        return literal1.sameValueAs(literal2);
    }

    public static boolean equivalentDatesIgnoringTimes(Literal literal1, Literal literal2) {
        RDFDatatype type1 = literal1.getDatatype();
        RDFDatatype type2 = literal2.getDatatype();
        if ((type1 instanceof XSDDateType || type1 instanceof XSDDateTimeType)
                && (type2 instanceof XSDDateType || type2 instanceof XSDDateTimeType)) {
            XSDDateTime date1 = ((XSDDateTime) literal1.getValue());
            XSDDateTime date2 = ((XSDDateTime) literal2.getValue());
            return date1.getDays() == date2.getDays() //
                    && date1.getMonths() == date2.getMonths() //
                    && date1.getYears() == date2.getYears();
        }
        return false;
    }

    public static boolean equivalentStringsIgnoringLangTag(Literal literal1, Literal literal2) {
        RDFDatatype type1 = literal1.getDatatype();
        RDFDatatype type2 = literal2.getDatatype();
        if ((type1 instanceof XSDBaseStringType || type1 instanceof RDFLangString)
                && (type2 instanceof XSDBaseStringType || type2 instanceof RDFLangString)) {
            String string1 = literal1.getString();
            String string2 = literal2.getString();
            return Objects.equals(string1, string2);
        }
        return false;
    }

    public static boolean equivalentNumbersIgnoringNumberType(Literal literal1, Literal literal2) {
        try {
            return equivalentSpecialNumbersIgnoringNumberType(literal1, literal2) ||
                    equivalentNonSpecialNumbersIgnoringNumberType(literal1, literal2);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    protected static boolean equivalentSpecialNumbersIgnoringNumberType(Literal literal1, Literal literal2) {
        RDFDatatype type1 = literal1.getDatatype();
        RDFDatatype type2 = literal2.getDatatype();
        if (type1 instanceof XSDDouble) {
            double value1 = literal1.getDouble();
            if (type2 instanceof XSDDouble) {
                double value2 = literal2.getDouble();
                return Double.isNaN(value1) && Double.isNaN(value2) || Double.isInfinite(value1) && value1 == value2;
            }
            if (type2 instanceof XSDFloat) {
                float value2 = literal2.getFloat();
                return Double.isNaN(value1) && Float.isNaN(value2) || Double.isInfinite(value1) && value1 == value2;
            }
        }
        if (type1 instanceof XSDFloat) {
            float value1 = literal1.getFloat();
            if (type2 instanceof XSDDouble) {
                double value2 = literal2.getDouble();
                return Float.isNaN(value1) && Double.isNaN(value2) || Float.isInfinite(value1) && value1 == value2;
            }
            if (type2 instanceof XSDFloat) {
                float value2 = literal2.getFloat();
                return Float.isNaN(value1) && Float.isNaN(value2) || Float.isInfinite(value1) && value1 == value2;
            }
        }
        return false;
    }

    protected static boolean equivalentNonSpecialNumbersIgnoringNumberType(Literal literal1, Literal literal2) {
        BigDecimal decimal1 = numberLiteralAsBigDecimal(literal1);
        BigDecimal decimal2 = numberLiteralAsBigDecimal(literal2);
        return decimal1.compareTo(decimal2) == 0;
    }

    protected static BigDecimal numberLiteralAsBigDecimal(Literal literal) {
        RDFDatatype type = literal.getDatatype();
        if (type instanceof XSDBaseNumericType) {
            return xsdDecimalLiteralAsBigDecimal(literal);
        } else if (type instanceof XSDDouble) {
            return xsdDoubleLiteralAsBigDecimal(literal);
        } else if (type instanceof XSDFloat) {
            return xsdFloatLiteralAsBigDecimal(literal);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected static BigDecimal xsdDecimalLiteralAsBigDecimal(Literal literal) {
        String lexical = literal.getLexicalForm();
        return new BigDecimal(lexical);
    }

    protected static BigDecimal xsdFloatLiteralAsBigDecimal(Literal literal) {
        float floatValue = literal.getFloat();
        return new BigDecimal(floatValue);
        // NOTE: do not use BigDecimal#valueOf(floatValue)
        // NOTE: do not use BigDecimal(literal.getLexicalForm())
    }

    protected static BigDecimal xsdDoubleLiteralAsBigDecimal(Literal literal) {
        double doubleValue = literal.getDouble();
        return new BigDecimal(doubleValue);
        // NOTE: do not use BigDecimal#valueOf(doubleValue)
        // NOTE: do not use BigDecimal(literal.getLexicalForm())
    }

}
