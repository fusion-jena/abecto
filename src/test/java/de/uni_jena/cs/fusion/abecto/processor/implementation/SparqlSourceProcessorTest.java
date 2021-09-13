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
package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.Optional;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.processor.SparqlSourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SparqlSourceProcessor.Parameter;

public class SparqlSourceProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// scope
		final int hierarchyDepth = 5;
		final int maxAssociatenDistance = 5;

		// generate test data
		Dataset testData = DatasetFactory.createTxnMem();
		String namespace = "http://example.org/";

		Node association = NodeFactory.createURI(namespace + "association");
		Node inverseAssociation = NodeFactory.createURI(namespace + "inverseAssociation");
		testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
				NodeFactory.createURI(namespace + "association"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label")));

		// individual
		testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label")));
		testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDF.type.asNode(), //
				NodeFactory.createURI(namespace + "class" + 0)));
		testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				association, //
				NodeFactory.createURI(namespace + "association" + 0)));
		testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
				NodeFactory.createURI(namespace + "inverseAssociation" + 0), //
				inverseAssociation, //
				NodeFactory.createURI(namespace + "individual")));

		// hierarchy
		for (int i = 0; i < hierarchyDepth; i++) {
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "class" + (i + 1))));
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i + "Sibling"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "class" + (i + 1))));
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i + "Sibling"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createLiteral("label")));

			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i), //
					association, //
					NodeFactory.createURI(namespace + "class" + i + "Association" + 0)));
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "class" + i + "Association" + 0), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));
		}

		// associations
		for (int i = 0; i < maxAssociatenDistance; i++) {
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "association" + i), //
					association, //
					NodeFactory.createURI(namespace + "association" + (i + 1))));
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "association" + i), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + (i + 1)), //
					inverseAssociation, //
					NodeFactory.createURI(namespace + "inverseAssociation" + i)));
			testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + i), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			for (int j = 0; j < hierarchyDepth; j++) {
				testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
						NodeFactory.createURI(namespace + "class" + j + "Association" + i), //
						association, //
						NodeFactory.createURI(namespace + "class" + j + "Association" + (i + 1))));
				testData.asDatasetGraph().getDefaultGraph().add(new Triple(//
						NodeFactory.createURI(namespace + "class" + j + "Association" + i), //
						RDFS.label.asNode(), //
						NodeFactory.createLiteral("label")));
			}
		}

		// run Fuseki server to provide SPARQL endpoint on HTTP with test data
		// see: https://jena.apache.org/documentation/fuseki2/fuseki-embedded.html
		FusekiServer fuseki = FusekiServer.create().port(0).add("/test", testData).build().start();
		// Workaround for JENA-2042 https://issues.apache.org/jira/browse/JENA-2042
		// int port = fuseki.getPort();
		int port = ((ServerConnector) fuseki.getJettyServer().getConnectors()[0]).getLocalPort();

		// run queries against test endpoint

		Parameter parameter = new Parameter();
		parameter.service = "http://localhost:" + port + "/test/sparql";
		parameter.query = Optional.of(QueryFactory.create("SELECT ?item WHERE {?item a <" + namespace + "class0>.}"));
		parameter.inverseAssociationProperties = Collections.singletonList(inverseAssociation);
		SparqlSourceProcessor processor = new SparqlSourceProcessor();
		processor.setParameters(parameter);

		for (int ali = 0; ali < maxAssociatenDistance; ali++) {
			parameter.associatedLoadIterations = ali;
			Model outputModel = processor.call();

			// individual
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label"))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					RDF.type.asNode(), //
					NodeFactory.createURI(namespace + "class" + 0))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					association, //
					NodeFactory.createURI(namespace + "association" + 0))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + 0), //
					inverseAssociation, //
					NodeFactory.createURI(namespace + "individual"))));

			// hierarchy
			for (int i = 0; i < hierarchyDepth; i++) {
				assertTrue(ali == 0 ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + i), //
								RDFS.subClassOf.asNode(), //
								NodeFactory.createURI(namespace + "class" + (i + 1)))));
				assertTrue(ali == 0 ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + 0), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "class" + i + "Sibling"), //
						RDFS.subClassOf.asNode(), //
						NodeFactory.createURI(namespace + "class" + (i + 1)))));
				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "class" + i + "Sibling"), //
						RDFS.label.asNode(), //
						NodeFactory.createLiteral("label"))));

				assertTrue(ali == 0 ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + i), //
								association, //
								NodeFactory.createURI(namespace + "class" + i + "Association" + 0))));
				assertTrue(i > ali - 2 ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + i + "Association" + 0), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));
			}

			// associations
			for (int i = 0; i < maxAssociatenDistance; i++) {
				assertTrue(i >= ali ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + i), //
								association, //
								NodeFactory.createURI(namespace + "association" + (i + 1)))));
				assertTrue(i >= ali ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + i), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				assertTrue(i >= ali ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "inverseAssociation" + (i + 1)), //
								inverseAssociation, //
								NodeFactory.createURI(namespace + "inverseAssociation" + i))));
				assertTrue(i >= ali ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "inverseAssociation" + i), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				for (int j = 0; j < hierarchyDepth; j++) {
					assertTrue(i + j + 2 > ali ^ //
							outputModel.getGraph().contains(new Triple(//
									NodeFactory.createURI(namespace + "class" + j + "Association" + i), //
									association, //
									NodeFactory.createURI(namespace + "class" + j + "Association" + (i + 1)))));
					assertTrue(i + j + 2 > ali ^ //
							outputModel.getGraph().contains(new Triple(//
									NodeFactory.createURI(namespace + "class" + j + "Association" + i), //
									RDFS.label.asNode(), //
									NodeFactory.createLiteral("label"))));
				}
			}
		}

		fuseki.stop();
	}
}
