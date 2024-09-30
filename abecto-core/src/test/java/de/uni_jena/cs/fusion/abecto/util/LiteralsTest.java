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
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LiteralsTest {

    @Test
    public void equivalentNumbersIgnoringNumberType() {
        // non-special numbers of integer and others
        assertEquivalentNumbers("-5",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDinteger);
        assertEquivalentNumbers("-5",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDdecimal);
        assertEquivalentNumbers("-5",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("-5",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDdouble);

        // non-special numbers of decimal and others
        assertEquivalentNumbers("-5.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDdecimal);
        assertEquivalentNumbers("-5.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("-5.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDdouble);

        // non-special numbers of float and float
        assertEquivalentNumbers("4.2E9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e9",XSDDatatype.XSDfloat,"4.2e9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2E0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e0",XSDDatatype.XSDfloat,"4.2e0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2E-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("4.2e-9",XSDDatatype.XSDfloat,"4.2e-9",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("0.0042",XSDDatatype.XSDfloat,"0.0042E0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("0.0042",XSDDatatype.XSDfloat,"0.0042e0",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("0.0042",XSDDatatype.XSDfloat,"4.2e-3",XSDDatatype.XSDfloat);

        // non-special numbers of float and double beyond granted precision
        assertEquivalentNumbers("4.2E9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("4.2E9",XSDDatatype.XSDdouble,"4.2E9",XSDDatatype.XSDdouble);

        // special numbers of float and double
        assertEquivalentNumbers("NaN",XSDDatatype.XSDdouble,"NaN",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("NaN",XSDDatatype.XSDfloat,"NaN",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("NaN",XSDDatatype.XSDdouble,"NaN",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("NaN",XSDDatatype.XSDfloat,"NaN",XSDDatatype.XSDfloat);

        assertEquivalentNumbers("-INF",XSDDatatype.XSDdouble,"-INF",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("-INF",XSDDatatype.XSDfloat,"-INF",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("-INF",XSDDatatype.XSDdouble,"-INF",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("-INF",XSDDatatype.XSDfloat,"-INF",XSDDatatype.XSDfloat);

        assertEquivalentNumbers("INF",XSDDatatype.XSDdouble,"INF",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("INF",XSDDatatype.XSDfloat,"INF",XSDDatatype.XSDdouble);
        assertEquivalentNumbers("INF",XSDDatatype.XSDdouble,"INF",XSDDatatype.XSDfloat);
        assertEquivalentNumbers("INF",XSDDatatype.XSDfloat,"INF",XSDDatatype.XSDfloat);

        // non-special numbers of double and double
        assertNotEquivalentNumbers("INF",XSDDatatype.XSDdouble,"-INF",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("INF",XSDDatatype.XSDfloat,"-INF",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("INF",XSDDatatype.XSDdouble,"-INF",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("INF",XSDDatatype.XSDfloat,"-INF",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("-INF",XSDDatatype.XSDdouble,"INF",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("-INF",XSDDatatype.XSDfloat,"INF",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("-INF",XSDDatatype.XSDdouble,"INF",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("-INF",XSDDatatype.XSDfloat,"INF",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("true",XSDDatatype.XSDboolean,"false",XSDDatatype.XSDboolean);

        // non-special numbers of integer and others
        assertNotEquivalentNumbers("-4",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDinteger);
        assertNotEquivalentNumbers("-4",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDdecimal);
        assertNotEquivalentNumbers("-4",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("-4",XSDDatatype.XSDinteger,"-5",XSDDatatype.XSDdouble);

        // non-special numbers of decimal and others
        assertNotEquivalentNumbers("-4.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDdecimal);
        assertNotEquivalentNumbers("-4.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("-4.0",XSDDatatype.XSDdecimal,"-5.0",XSDDatatype.XSDdouble);

        // non-special numbers of float and float
        assertNotEquivalentNumbers("3.2E9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDfloat,"4.2e9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2E0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDfloat,"4.2e0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2E-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDfloat,"4.2e-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"0.0042E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"0.0042e0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"4.2e-3",XSDDatatype.XSDfloat);

        // non-special numbers of float and double
        assertNotEquivalentNumbers("3.2E9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDfloat,"4.2E9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDfloat,"4.2e9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2E0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDfloat,"4.2E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDfloat,"4.2e0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2E-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDfloat,"4.2E-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDfloat,"4.2e-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"0.0042E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"0.0042e0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDfloat,"4.2e-3",XSDDatatype.XSDdouble);

        // non-special numbers of double and float
        assertNotEquivalentNumbers("3.2E9",XSDDatatype.XSDdouble,"4.2E9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDdouble,"4.2E9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDdouble,"4.2e9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2E0",XSDDatatype.XSDdouble,"4.2E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDdouble,"4.2E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDdouble,"4.2e0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2E-9",XSDDatatype.XSDdouble,"4.2E-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDdouble,"4.2E-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDdouble,"4.2e-9",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"0.0042E0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"0.0042e0",XSDDatatype.XSDfloat);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"4.2e-3",XSDDatatype.XSDfloat);

        // non-special numbers of double and double
        assertNotEquivalentNumbers("3.2E9",XSDDatatype.XSDdouble,"4.2E9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDdouble,"4.2E9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e9",XSDDatatype.XSDdouble,"4.2e9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2E0",XSDDatatype.XSDdouble,"4.2E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDdouble,"4.2E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e0",XSDDatatype.XSDdouble,"4.2e0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2E-9",XSDDatatype.XSDdouble,"4.2E-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDdouble,"4.2E-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("3.2e-9",XSDDatatype.XSDdouble,"4.2e-9",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"0.0042E0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"0.0042e0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.0032",XSDDatatype.XSDdouble,"4.2e-3",XSDDatatype.XSDdouble);

        // non-special numbers of float and double beyond granted precision
        assertNotEquivalentNumbers("0.001",XSDDatatype.XSDfloat,"0.001e0",XSDDatatype.XSDdouble);
        assertNotEquivalentNumbers("0.001",XSDDatatype.XSDdouble,"0.001e0",XSDDatatype.XSDfloat);
    }

    void assertEquivalentNumbers(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertTrue(Literals.equivalentNumbersIgnoringNumberType(literal1, literal2));
    }

    void assertNotEquivalentNumbers(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertFalse(Literals.equivalentNumbersIgnoringNumberType(literal1, literal2));
    }

    @Test
    void equivalentLiteralsOfSameTypes() throws Exception {
        assertEquivalentLiteralsOfSameType("true",XSDDatatype.XSDboolean,"true",XSDDatatype.XSDboolean);
        assertEquivalentLiteralsOfSameType("value",null,"value",null);
        assertNotEquivalentLiteralsOfSameType("false",XSDDatatype.XSDboolean,"true",XSDDatatype.XSDboolean);
        assertNotEquivalentLiteralsOfSameType("value1",null,"value2",null);
    }

    void assertEquivalentLiteralsOfSameType(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertTrue(Literals.equivalentLiteralsOfSameTypes(literal1, literal2));
    }

    void assertNotEquivalentLiteralsOfSameType(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertFalse(Literals.equivalentLiteralsOfSameTypes(literal1, literal2));
    }

    @Test
    void equivalentDatesIgnoringTimes() throws Exception {
        assertEquivalentDatesIgnoringTimes("2000-01-01",XSDDatatype.XSDdate,"2000-01-01T00:00:00",XSDDatatype.XSDdateTime);
        assertEquivalentDatesIgnoringTimes("2000-01-01",XSDDatatype.XSDdate,"2000-01-01T17:00:00",XSDDatatype.XSDdateTime);
        assertNotEquivalentDatesIgnoringTimes("2000-01-02",XSDDatatype.XSDdate,"2000-01-01T00:00:00",XSDDatatype.XSDdateTime);
        assertNotEquivalentDatesIgnoringTimes("2000-01-02",XSDDatatype.XSDdate,"2000-01-01T17:00:00",XSDDatatype.XSDdateTime);
    }

    void assertEquivalentDatesIgnoringTimes(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertTrue(Literals.equivalentDatesIgnoringTimes(literal1, literal2));
        Assertions.assertTrue(Literals.equivalentDatesIgnoringTimes(literal2, literal1));
    }

    void assertNotEquivalentDatesIgnoringTimes(String lexical1, RDFDatatype type1 , String lexical2, RDFDatatype type2) {
        Literal literal1 = ResourceFactory.createTypedLiteral(lexical1, type1);
        Literal literal2 = ResourceFactory.createTypedLiteral(lexical2, type2);
        Assertions.assertFalse(Literals.equivalentLiteralsOfSameTypes(literal1, literal2));
        Assertions.assertFalse(Literals.equivalentLiteralsOfSameTypes(literal2, literal1));
    }

    @Test
    void equivalentDatesStringsIgnoringLangTag() throws Exception {
        assertEquivalentStringsIgnoringLangTag("value","en","value","en");
        assertEquivalentStringsIgnoringLangTag("value","en","value","de");
        assertEquivalentStringsIgnoringLangTag("value","en","value",null);
        assertEquivalentStringsIgnoringLangTag("value","de","value",null);
        assertNotEquivalentStringsIgnoringLangTag("value1","en","value2","en");
        assertNotEquivalentStringsIgnoringLangTag("value1","en","value2","de");
        assertNotEquivalentStringsIgnoringLangTag("value1","en","value2",null);
        assertNotEquivalentStringsIgnoringLangTag("value1","de","value2",null);
    }

    void assertEquivalentStringsIgnoringLangTag(String lexical1, String lang1 , String lexical2, String lang2) {
        Literal literal1, literal2;
        if (lang1!=null) {
            literal1 = ResourceFactory.createLangLiteral(lexical1, lang1);
        } else {
            literal1 = ResourceFactory.createStringLiteral(lexical1);
        }
        if (lang2!=null) {
            literal2 = ResourceFactory.createLangLiteral(lexical2, lang2);
        } else {
            literal2 = ResourceFactory.createStringLiteral(lexical2);
        }
        Assertions.assertTrue(Literals.equivalentStringsIgnoringLangTag(literal1, literal2));
        Assertions.assertTrue(Literals.equivalentStringsIgnoringLangTag(literal2, literal1));
    }

    void assertNotEquivalentStringsIgnoringLangTag(String lexical1, String lang1 , String lexical2, String lang2) {
        Literal literal1, literal2;
        if (lang1!=null) {
            literal1 = ResourceFactory.createLangLiteral(lexical1, lang1);
        } else {
            literal1 = ResourceFactory.createStringLiteral(lexical1);
        }
        if (lang2!=null) {
            literal2 = ResourceFactory.createLangLiteral(lexical2, lang2);
        } else {
            literal2 = ResourceFactory.createStringLiteral(lexical2);
        }
        Assertions.assertFalse(Literals.equivalentStringsIgnoringLangTag(literal1, literal2));
        Assertions.assertFalse(Literals.equivalentStringsIgnoringLangTag(literal2, literal1));
    }
}
