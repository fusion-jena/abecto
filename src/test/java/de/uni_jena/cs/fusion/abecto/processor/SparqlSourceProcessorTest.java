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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestUtil;

public class SparqlSourceProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// scope
		final int maxFollowUnlimitedDistance = 5;
		final int maxMaxDistance = 5;

		// generate test data
		Dataset testData = DatasetFactory.createTxnMem();
		Graph inputGraph = testData.asDatasetGraph().getDefaultGraph();
		String namespace = "http://example.org/";

		Resource association = ResourceFactory.createResource(namespace + "association");
		Property inverseAssociation = ResourceFactory.createProperty(namespace + "inverseAssociation");
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "association"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label")));

		// individual
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label")));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				association.asNode(), //
				NodeFactory.createURI(namespace + "association" + 1)));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "inverseAssociation" + 1), //
				inverseAssociation.asNode(), //
				NodeFactory.createURI(namespace + "individual")));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDFS.subClassOf.asNode(), //
				NodeFactory.createURI(namespace + "followUnlimited" + 1)));
		// literals with language
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label", "en")));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				RDFS.label.asNode(), //
				NodeFactory.createLiteral("label", "de")));

		// followUnlimited
		for (

				int followUnlimitedDistance = 1; followUnlimitedDistance <= maxFollowUnlimitedDistance; followUnlimitedDistance++) {
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "followUnlimited" + (followUnlimitedDistance + 1))));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance), //
					association.asNode(), //
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Association")));

			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance)));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
					association.asNode(), //
					NodeFactory.createURI(
							namespace + "followUnlimited" + followUnlimitedDistance + "Sibling" + "Association")));
		}

		// associations
		for (int distance = 1; distance <= maxMaxDistance; distance++) {
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "association" + distance), //
					association.asNode(), //
					NodeFactory.createURI(namespace + "association" + (distance + 1))));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "association" + distance), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + (distance + 1)), //
					inverseAssociation.asNode(), //
					NodeFactory.createURI(namespace + "inverseAssociation" + distance)));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + distance), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));
		}

		// disable processor logging
		java.util.logging.Logger.getLogger(SparqlSourceProcessor.class.getCanonicalName())
				.setLevel(java.util.logging.Level.OFF);
		// disable fuseki logging
		LogCtl.setLevel(Fuseki.serverLogName, "OFF");
		LogCtl.setLevel(Fuseki.actionLogName, "OFF");
		LogCtl.setLevel(Fuseki.requestLogName, "OFF");
		LogCtl.setLevel(Fuseki.adminLogName, "OFF");
		LogCtl.setLevel("org.eclipse.jetty", "OFF");
		// run Fuseki server to provide SPARQL endpoint on HTTP with test data
		// see: https://jena.apache.org/documentation/fuseki2/fuseki-embedded.html
		FusekiServer fuseki = FusekiServer.create().port(0).add("/test", testData).build().start();

		for (int maxDistance = 0; maxDistance <= maxMaxDistance; maxDistance++) {
			// run queries against test endpoint
			SparqlSourceProcessor processor = new SparqlSourceProcessor();
			processor.service = ResourceFactory.createResource("http://localhost:" + fuseki.getPort() + "/test/sparql");
			processor.query = Optional
					.of(QueryFactory.create("SELECT ?item WHERE {BIND(<" + namespace + "individual> AS ?item)}"));
			processor.followInverse = Collections.singletonList(inverseAssociation);
			processor.setAssociatedDataset(TestUtil.dataset(1));
			processor.maxDistance = maxDistance;
			processor.run();
			Model outputModel = processor.getOutputPrimaryModel().get();

			// individual
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label"))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "followUnlimited" + 1))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					association.asNode(), //
					NodeFactory.createURI(namespace + "association" + 1))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + 1), //
					inverseAssociation.asNode(), //
					NodeFactory.createURI(namespace + "individual"))));

			// hierarchy
			for (int followUnlimitedDistance = 1; followUnlimitedDistance <= maxFollowUnlimitedDistance; followUnlimitedDistance++) {
				assertTrue(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance), //
						RDFS.subClassOf.asNode(), //
						NodeFactory.createURI(namespace + "followUnlimited" + (followUnlimitedDistance + 1)))));
				assertTrue(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + 1), //
						RDFS.label.asNode(), //
						NodeFactory.createLiteral("label"))));
				assertTrue(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance), //
						association.asNode(), //
						NodeFactory
								.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Association"))));

				assertTrue(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
						RDFS.subClassOf.asNode(), //
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance))),
						"followUnlimitedDistance=" + followUnlimitedDistance + " maxDistance=" + maxDistance);
				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
						RDFS.label.asNode(), //
						NodeFactory.createLiteral("label"))),
						"followUnlimitedDistance=" + followUnlimitedDistance + " maxDistance=" + maxDistance);
				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "followUnlimited" + followUnlimitedDistance + "Sibling"), //
						RDFS.label.asNode(), //
						NodeFactory.createURI(
								namespace + "followUnlimited" + followUnlimitedDistance + "Sibling" + "Association"))),
						"followUnlimitedDistance=" + followUnlimitedDistance + " maxDistance=" + maxDistance);

			}

			// associations
			for (int distance = 1; distance < maxMaxDistance + 1; distance++) {
				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + distance), //
								association.asNode(), //
								NodeFactory.createURI(namespace + "association" + (distance + 1)))));
				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + distance), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "inverseAssociation" + (distance + 1)), //
								inverseAssociation.asNode(), //
								NodeFactory.createURI(namespace + "inverseAssociation" + distance))));
				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "inverseAssociation" + distance), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));
			}
		}

		fuseki.stop();
	}
}
