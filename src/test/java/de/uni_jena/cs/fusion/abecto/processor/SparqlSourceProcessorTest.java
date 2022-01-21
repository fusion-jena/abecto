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
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestUtil;

public class SparqlSourceProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		// scope
		final int maxHierarchyDepth = 5;
		final int maxMaxDistance = 5;

		// generate test data
		Dataset testData = DatasetFactory.createTxnMem();
		Graph inputGraph = testData.asDatasetGraph().getDefaultGraph();
		String namespace = "http://example.org/";

		Node association = NodeFactory.createURI(namespace + "association");
		Node inverseAssociation = NodeFactory.createURI(namespace + "inverseAssociation");
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
				RDF.type.asNode(), //
				NodeFactory.createURI(namespace + "class" + 0)));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "individual"), //
				association, //
				NodeFactory.createURI(namespace + "association" + 1)));
		inputGraph.add(new Triple(//
				NodeFactory.createURI(namespace + "inverseAssociation" + 1), //
				inverseAssociation, //
				NodeFactory.createURI(namespace + "individual")));

		// hierarchy
		for (int hierarchyDepth = 0; hierarchyDepth < maxHierarchyDepth; hierarchyDepth++) {
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "class" + hierarchyDepth), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "class" + (hierarchyDepth + 1))));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "class" + hierarchyDepth), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Sibling"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createURI(namespace + "class" + (hierarchyDepth + 1))));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Sibling"), //
					RDFS.subClassOf.asNode(), //
					NodeFactory.createLiteral("label")));

			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "class" + hierarchyDepth), //
					association, //
					NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Association" + 0)));
		}

		// associations
		for (int distance = 1; distance < maxMaxDistance + 1; distance++) {
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "association" + distance), //
					association, //
					NodeFactory.createURI(namespace + "association" + (distance + 1))));
			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "association" + distance), //
					RDFS.label.asNode(), //
					NodeFactory.createLiteral("label")));

			inputGraph.add(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + (distance + 1)), //
					inverseAssociation, //
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

		for (int maxDistance = 0; maxDistance < maxMaxDistance; maxDistance++) {
			// run queries against test endpoint
			SparqlSourceProcessor processor = new SparqlSourceProcessor();
			processor.service = "http://localhost:" + fuseki.getPort() + "/test/sparql";
			processor.query = Optional
					.of(QueryFactory.create("SELECT ?item WHERE {?item a <" + namespace + "class0>.}"));
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
					RDF.type.asNode(), //
					NodeFactory.createURI(namespace + "class" + 0))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "individual"), //
					association, //
					NodeFactory.createURI(namespace + "association" + 1))));
			assertTrue(outputModel.getGraph().contains(new Triple(//
					NodeFactory.createURI(namespace + "inverseAssociation" + 1), //
					inverseAssociation, //
					NodeFactory.createURI(namespace + "individual"))));

			// hierarchy
			for (int hierarchyDepth = 0; hierarchyDepth < maxHierarchyDepth; hierarchyDepth++) {
				assertTrue(maxDistance == 0 ^ // bridge the rdf:type property
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + hierarchyDepth), //
								RDFS.subClassOf.asNode(), //
								NodeFactory.createURI(namespace + "class" + (hierarchyDepth + 1)))));
				assertTrue(maxDistance == 0 ^ // bridge the rdf:type property
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + 0), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Sibling"), //
						RDFS.subClassOf.asNode(), //
						NodeFactory.createURI(namespace + "class" + (hierarchyDepth + 1)))));
				assertFalse(outputModel.getGraph().contains(new Triple(//
						NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Sibling"), //
						RDFS.label.asNode(), //
						NodeFactory.createLiteral("label"))));

				assertTrue(maxDistance == 0 ^ // bridge the rdf:type property
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "class" + hierarchyDepth), //
								association, //
								NodeFactory.createURI(namespace + "class" + hierarchyDepth + "Association" + 0))));
			}

			// associations
			for (int distance = 1; distance < maxMaxDistance + 1; distance++) {
				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + distance), //
								association, //
								NodeFactory.createURI(namespace + "association" + (distance + 1)))));
				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "association" + distance), //
								RDFS.label.asNode(), //
								NodeFactory.createLiteral("label"))));

				assertTrue(distance > maxDistance ^ //
						outputModel.getGraph().contains(new Triple(//
								NodeFactory.createURI(namespace + "inverseAssociation" + (distance + 1)), //
								inverseAssociation, //
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
