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
package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.DQV;
import de.uni_jena.cs.fusion.abecto.vocabulary.SdmxAttribute;

public class MetadataTest {
	Resource affectedDataset = ResourceFactory.createResource("http://example.org/affectedDataset");
	Resource affectedResource = ResourceFactory.createResource("http://example.org/affectedResource");
	String affectedVariableName = "affectedVariable";
	Literal affectedValueLiteral = ResourceFactory.createStringLiteral("affectedValueLiteral");
	Resource affectedValueResource = ResourceFactory.createResource("http://example.org/affectedValueResource");
	Resource computedOnDataset = ResourceFactory.createResource("http://example.org/computedOnDataset");
	Resource comparedToDataset = ResourceFactory.createResource("http://example.org/comparedToDataset");
	Resource comparedToDataset2 = ResourceFactory.createResource("http://example.org/comparedToDataset2");
	Resource comparedToDataset3 = ResourceFactory.createResource("http://example.org/comparedToDataset2");
	Resource comparedToResource = ResourceFactory.createResource("http://example.org/comparedToResource");
	Literal comparedToValueLiteral = ResourceFactory.createStringLiteral("comparedToValueLiteral");
	Resource comparedToValueResource = ResourceFactory.createResource("http://example.org/comparedToValueResource");
	Resource affectedAspect = ResourceFactory.createResource("http://example.org/affectedAspect");
	Resource unit = ResourceFactory.createResource("http://example.org/unit");
	Resource measure = ResourceFactory.createResource("http://example.org/measure");
	Integer value = 123;

	@Test
	public void addDeviation() {
		Model model = ModelFactory.createDefaultModel();
		Metadata.addDeviation(affectedResource, affectedVariableName, affectedValueLiteral, comparedToDataset,
				comparedToResource, comparedToValueLiteral, affectedAspect, model);
		Metadata.addDeviation(affectedResource, affectedVariableName, affectedValueResource, comparedToDataset,
				comparedToResource, comparedToValueResource, affectedAspect, model);
		Query queryLiteral = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?deviation a <" + AV.Deviation + "> ;\n"//
				+ "             <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "             <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
				+ "             <" + AV.affectedValue + "> \"" + affectedValueLiteral.toString() + "\" ;\n"//
				+ "             <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ;\n"//
				+ "             <" + AV.comparedToResource + "> <" + comparedToResource + "> ;\n"//
				+ "             <" + AV.comparedToValue + "> \"" + comparedToValueLiteral.toString() + "\" .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedResource + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?deviation .\n"//
				+ "}");
		Query queryResource = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?deviation a <" + AV.Deviation + "> ;\n"//
				+ "             <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "             <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
				+ "             <" + AV.affectedValue + "> <" + affectedValueResource + "> ;\n"//
				+ "             <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ;\n"//
				+ "             <" + AV.comparedToResource + "> <" + comparedToResource + "> ;\n"//
				+ "             <" + AV.comparedToValue + "> <" + comparedToValueResource + "> .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedResource + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?deviation .\n"//
				+ "}");
		assertTrue(QueryExecutionFactory.create(queryLiteral, model).execAsk());
		assertTrue(QueryExecutionFactory.create(queryResource, model).execAsk());
	}

	@Test
	public void addIssue() {
		Model model = ModelFactory.createDefaultModel();
		String issueType = "issueType";
		String comment = "comment";
		Metadata.addIssue(affectedResource, affectedVariableName, affectedValueLiteral, affectedAspect, issueType,
				comment, model);
		Query query = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?issue a <" + AV.Issue + "> ;\n"//
				+ "         <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "         <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
				+ "         <" + AV.affectedValue + "> \"" + affectedValueLiteral.toString() + "\" ;\n"//
				+ "         <" + AV.issueType + "> \"" + issueType + "\" ;\n"//
				+ "         <" + RDFS.comment + "> \"" + comment + "\" .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedResource + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?issue .\n"//
				+ "}");
		assertTrue(QueryExecutionFactory.create(query, model).execAsk());
	}

	@Test
	public void addResourceOmission() {
		Model model = ModelFactory.createDefaultModel();
		Metadata.addResourceOmission(affectedDataset, comparedToDataset, comparedToResource, affectedAspect, model);
		Query query = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?resourceOmission a <" + AV.ResourceOmission + "> ;\n"//
				+ "                    <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "                    <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ;\n"//
				+ "                    <" + AV.comparedToResource + "> <" + comparedToResource + "> .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedDataset + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?resourceOmission .\n"//
				+ "}");
		assertTrue(QueryExecutionFactory.create(query, model).execAsk());
	}

	@Test
	public void addValuesOmission() {
		Model model = ModelFactory.createDefaultModel();
		Metadata.addValuesOmission(affectedResource, affectedVariableName, comparedToDataset, comparedToResource,
				comparedToValueLiteral, affectedAspect, model);
		Metadata.addValuesOmission(affectedResource, affectedVariableName, comparedToDataset, comparedToResource,
				comparedToValueResource, affectedAspect, model);
		Query queryLiteral = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?valuesOmission a <" + AV.ValueOmission + "> ;\n"//
				+ "         <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "         <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
				+ "         <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ;\n"//
				+ "         <" + AV.comparedToResource + "> <" + comparedToResource + "> ;\n"//
				+ "         <" + AV.comparedToValue + "> \"" + comparedToValueLiteral + "\" .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedResource + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?valuesOmission .\n"//
				+ "}");
		Query queryResource = QueryFactory.create(""//
				+ "ASK WHERE {\n"//
				+ "  ?valuesOmission a <" + AV.ValueOmission + "> ;\n"//
				+ "         <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
				+ "         <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
				+ "         <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ;\n"//
				+ "         <" + AV.comparedToResource + "> <" + comparedToResource + "> ;\n"//
				+ "         <" + AV.comparedToValue + "> <" + comparedToValueResource + "> .\n"//
				+ "  ?qualityAnnotation a <" + DQV.QualityAnnotation + "> ;\n"//
				+ "                     <" + OA.hasTarget + "> <" + affectedResource + "> ;\n"//
				+ "                     <" + OA.hasBody + "> ?valuesOmission .\n"//
				+ "}");
		assertTrue(QueryExecutionFactory.create(queryLiteral, model).execAsk());
		assertTrue(QueryExecutionFactory.create(queryResource, model).execAsk());
	}

	@Test
	public void addQualityMeasurement() {
		Model model = ModelFactory.createDefaultModel();
		// no affected variable, empty comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset, affectedAspect, model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
		// no affected variable, one comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset, comparedToDataset, affectedAspect,
				model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
					+ "                      <" + AV.comparedToDataset + "> <" + comparedToDataset + "> .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
		// no affected variable, multiple comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset,
				Arrays.asList(comparedToDataset, comparedToDataset2, comparedToDataset3), affectedAspect, model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
					+ "                      <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ,\n"
					+ "                                                     <" + comparedToDataset2 + "> ,\n"
					+ "                                                     <" + comparedToDataset3 + "> .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
		// with affected variable, empty comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset, affectedVariableName, affectedAspect,
				model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
					+ "                      <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
		// with affected variable, one comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset, affectedVariableName, comparedToDataset,
				affectedAspect, model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
					+ "                      <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
					+ "                      <" + AV.comparedToDataset + "> <" + comparedToDataset + "> .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
		// with affected variable, multiple comparedToDatasets
		Metadata.addQualityMeasurement(measure, value, unit, computedOnDataset, affectedVariableName,
				Arrays.asList(comparedToDataset, comparedToDataset2, comparedToDataset3), affectedAspect, model);
		{
			Query query = QueryFactory.create(""//
					+ "ASK WHERE {\n"//
					+ "  ?qualityMeasurement a <" + AV.QualityMeasurement + "> ;\n"//
					+ "                      <" + DQV.isMeasurementOf + "> <" + measure + "> ;\n"//
					+ "                      <" + DQV.value + "> " + value.toString() + " ;\n"//
					+ "                      <" + SdmxAttribute.unitMeasure + "> <" + unit + "> ;\n"//
					+ "                      <" + AV.affectedAspect + "> <" + affectedAspect + "> ;\n"//
					+ "                      <" + AV.affectedVariableName + "> \"" + affectedVariableName + "\" ;\n"//
					+ "                      <" + AV.comparedToDataset + "> <" + comparedToDataset + "> ,\n"
					+ "                                                     <" + comparedToDataset2 + "> ,\n"
					+ "                                                     <" + comparedToDataset3 + "> .\n"//
					+ "}");
			assertTrue(QueryExecutionFactory.create(query, model).execAsk());
		}
	}

	@Test
	public void isWrongValue() {
		Literal affectedValue = ResourceFactory.createStringLiteral("affectedValue");
		Model model = ModelFactory.createDefaultModel();
		Resource wrongValue = model.createResource(AV.WrongValue)//
				.addProperty(AV.affectedAspect, affectedAspect)//
				.addLiteral(AV.affectedVariableName, affectedVariableName)//
				.addLiteral(AV.affectedValue, affectedValue);
		Resource qualityAnnotation = model.createResource(DQV.QualityAnnotation);
		qualityAnnotation.addProperty(OA.hasTarget, affectedResource);
		qualityAnnotation.addProperty(OA.hasBody, wrongValue);

		assertTrue(Metadata.isWrongValue(affectedResource, affectedVariableName, affectedValue, affectedAspect, model));
		assertFalse(Metadata.isWrongValue(ResourceFactory.createResource("http://example.org/otherResource"),
				affectedVariableName, affectedValue, affectedAspect, model));
		assertFalse(Metadata.isWrongValue(affectedResource, "otherVariableName", affectedValue, affectedAspect, model));
		assertFalse(Metadata.isWrongValue(affectedResource, affectedVariableName,
				ResourceFactory.createStringLiteral("otherValue"), affectedAspect, model));
		assertFalse(Metadata.isWrongValue(affectedResource, "otherVariableName", affectedValue,
				ResourceFactory.createResource("http://example.org/otherAspect"), model));
	}
}
