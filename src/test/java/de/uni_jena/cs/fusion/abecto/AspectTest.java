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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class AspectTest {

	@Test
	public void contructor() {
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Aspect aspect = new Aspect(aspectIri, "key");
		assertEquals(aspectIri, aspect.getIri());
		assertEquals("key", aspect.getKeyVariableName());
		assertEquals(Var.alloc("key"), aspect.getKeyVariable());
	}

	@Test
	public void getAspect() {
		Abecto.initApacheJena();

		Resource dataset1 = ResourceFactory.createResource("http://example.org/dataset1");
		Resource dataset2 = ResourceFactory.createResource("http://example.org/dataset2");

		Model configurationModel = ModelFactory.createDefaultModel();
		Resource aspect1Resource = configurationModel.createResource("http://example.org/aspect1", AV.Aspect);
		aspect1Resource.addLiteral(AV.keyVariableName, "key");
		Resource aspectPattern1Resource = configurationModel.createResource("http://example.org/aspectPattern1",
				AV.Aspect);
		aspectPattern1Resource.addProperty(AV.ofAspect, aspect1Resource);
		aspectPattern1Resource.addProperty(AV.associatedDataset, dataset1);
		Query pattern1 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property1> ?value .}");
		aspectPattern1Resource.addLiteral(AV.definingQuery, pattern1);
		Resource aspectPattern2Resource = configurationModel.createResource("http://example.org/aspectPattern2",
				AV.Aspect);
		aspectPattern2Resource.addProperty(AV.ofAspect, aspect1Resource);
		aspectPattern2Resource.addProperty(AV.associatedDataset, dataset2);
		Query pattern2 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property2> ?value .}");
		aspectPattern2Resource.addLiteral(AV.definingQuery, pattern2);

		Resource aspect2Resource = configurationModel.createResource("http://example.org/aspect2", AV.Aspect);
		aspect2Resource.addLiteral(AV.keyVariableName, "key");
		Resource aspectPattern3Resource = configurationModel.createResource("http://example.org/aspectPattern3",
				AV.Aspect);
		aspectPattern3Resource.addProperty(AV.ofAspect, aspect2Resource);
		aspectPattern3Resource.addProperty(AV.associatedDataset, dataset1);
		Query pattern3 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property3> ?value .}");
		aspectPattern3Resource.addLiteral(AV.definingQuery, pattern3);
		Resource aspectPattern4Resource = configurationModel.createResource("http://example.org/aspectPattern4",
				AV.Aspect);
		aspectPattern4Resource.addProperty(AV.ofAspect, aspect2Resource);
		aspectPattern4Resource.addProperty(AV.associatedDataset, dataset2);
		Query pattern4 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property4> ?value .}");
		aspectPattern4Resource.addLiteral(AV.definingQuery, pattern4);

		Aspect aspect1 = Aspect.getAspect(configurationModel, aspect1Resource);
		Aspect aspect2 = Aspect.getAspect(configurationModel, aspect2Resource);

		assertEquals(aspect1Resource, aspect1.getIri());
		assertEquals(aspect2Resource, aspect2.getIri());
		assertEquals("key", aspect1.getKeyVariableName());
		assertEquals("key", aspect2.getKeyVariableName());
		assertEquals(Var.alloc("key"), aspect1.getKeyVariable());
		assertEquals(Var.alloc("key"), aspect2.getKeyVariable());
		assertEquals(pattern1, aspect1.getPattern(dataset1));
		assertEquals(pattern2, aspect1.getPattern(dataset2));
		assertEquals(pattern3, aspect2.getPattern(dataset1));
		assertEquals(pattern4, aspect2.getPattern(dataset2));
	}

	@Test
	public void getAspects() {
		Abecto.initApacheJena();

		Resource dataset1 = ResourceFactory.createResource("http://example.org/dataset1");
		Resource dataset2 = ResourceFactory.createResource("http://example.org/dataset2");

		Model configurationModel = ModelFactory.createDefaultModel();
		Resource aspect1Resource = configurationModel.createResource("http://example.org/aspect1", AV.Aspect);
		aspect1Resource.addLiteral(AV.keyVariableName, "key");
		Resource aspectPattern1Resource = configurationModel.createResource("http://example.org/aspectPattern1",
				AV.AspectPattern);
		aspectPattern1Resource.addProperty(AV.ofAspect, aspect1Resource);
		aspectPattern1Resource.addProperty(AV.associatedDataset, dataset1);
		Query pattern1 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property1> ?value .}");
		aspectPattern1Resource.addLiteral(AV.definingQuery, pattern1);
		Resource aspectPattern2Resource = configurationModel.createResource("http://example.org/aspectPattern2",
				AV.AspectPattern);
		aspectPattern2Resource.addProperty(AV.ofAspect, aspect1Resource);
		aspectPattern2Resource.addProperty(AV.associatedDataset, dataset2);
		Query pattern2 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property2> ?value .}");
		aspectPattern2Resource.addLiteral(AV.definingQuery, pattern2);

		Resource aspect2Resource = configurationModel.createResource("http://example.org/aspect2", AV.Aspect);
		aspect2Resource.addLiteral(AV.keyVariableName, "key");
		Resource aspectPattern3Resource = configurationModel.createResource("http://example.org/aspectPattern3",
				AV.AspectPattern);
		aspectPattern3Resource.addProperty(AV.ofAspect, aspect2Resource);
		aspectPattern3Resource.addProperty(AV.associatedDataset, dataset1);
		Query pattern3 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property3> ?value .}");
		aspectPattern3Resource.addLiteral(AV.definingQuery, pattern3);
		Resource aspectPattern4Resource = configurationModel.createResource("http://example.org/aspectPattern4",
				AV.AspectPattern);
		aspectPattern4Resource.addProperty(AV.ofAspect, aspect2Resource);
		aspectPattern4Resource.addProperty(AV.associatedDataset, dataset2);
		Query pattern4 = QueryFactory.create("SELECT ?key ?value WHERE {?key <http://example.org/property4> ?value .}");
		aspectPattern4Resource.addLiteral(AV.definingQuery, pattern4);

		Map<Resource, Aspect> aspects = Aspect.getAspects(configurationModel);
		Aspect aspect1 = aspects.get(aspect1Resource);
		Aspect aspect2 = aspects.get(aspect2Resource);

		assertEquals(aspect1Resource, aspect1.getIri());
		assertEquals(aspect2Resource, aspect2.getIri());
		assertEquals("key", aspect1.getKeyVariableName());
		assertEquals("key", aspect2.getKeyVariableName());
		assertEquals(Var.alloc("key"), aspect1.getKeyVariable());
		assertEquals(Var.alloc("key"), aspect2.getKeyVariable());
		assertEquals(pattern1, aspect1.getPattern(dataset1));
		assertEquals(pattern2, aspect1.getPattern(dataset2));
		assertEquals(pattern3, aspect2.getPattern(dataset1));
		assertEquals(pattern4, aspect2.getPattern(dataset2));
	}

	@Test
	public void getResource() {
		// TODO
	}

	@Test
	public void getResourceIndex() {
		Abecto.initApacheJena();

		Model primaryDataModel = ModelFactory.createDefaultModel();
		Property property = ResourceFactory.createProperty("http://example.org/property");
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		Resource resoutce1 = ResourceFactory.createResource("http://example.org/1");
		Resource resoutce2 = ResourceFactory.createResource("http://example.org/2");
		Resource resoutce3 = ResourceFactory.createResource("http://example.org/3");
		Resource resoutce4 = ResourceFactory.createResource("http://example.org/4");
		primaryDataModel.addLiteral(resoutce1, property, 1);
		primaryDataModel.addLiteral(resoutce2, property, 2);
		primaryDataModel.addLiteral(resoutce3, property, 3);
		primaryDataModel.addLiteral(resoutce4, property, 4);
		Aspect aspect = new Aspect(aspectIri, "key");
		Query pattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property.getURI() + "> ?value .}");
		aspect.setPattern(dataset, pattern);

		// without modifier

		Map<String, Map<RDFNode, Set<Resource>>> index1 = Aspect.getResourceIndex(aspect, dataset,
				Collections.singleton("value"), primaryDataModel);

		assertTrue(index1.get("value").keySet().contains(ResourceFactory.createTypedLiteral(1)));
		assertTrue(index1.get("value").keySet().contains(ResourceFactory.createTypedLiteral(2)));
		assertTrue(index1.get("value").keySet().contains(ResourceFactory.createTypedLiteral(3)));
		assertTrue(index1.get("value").keySet().contains(ResourceFactory.createTypedLiteral(4)));

		for (RDFNode value : index1.get("value").keySet()) {
			switch (value.asLiteral().getInt()) {
			case 1:
				assertTrue(index1.get("value").get(value).contains(resoutce1));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 2:
				assertTrue(index1.get("value").get(value).contains(resoutce2));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 3:
				assertTrue(index1.get("value").get(value).contains(resoutce3));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 4:
				assertTrue(index1.get("value").get(value).contains(resoutce4));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			default:
				fail("Unexpected value.");
			}
		}

		// without modifier

		Map<String, Map<Integer, Set<Resource>>> index2 = Aspect.getResourceIndex(aspect, dataset,
				Collections.singleton("value"), primaryDataModel, l -> (l.asLiteral().getInt() % 2));

		assertTrue(index2.get("value").keySet().contains(0));
		assertTrue(index2.get("value").keySet().contains(1));

		for (Integer value : index2.get("value").keySet()) {
			switch (value.intValue()) {
			case 0:
				assertTrue(index2.get("value").get(value).contains(resoutce2));
				assertTrue(index2.get("value").get(value).contains(resoutce4));
				assertEquals(2, index2.get("value").get(value).size());
				break;
			case 1:
				assertTrue(index2.get("value").get(value).contains(resoutce1));
				assertTrue(index2.get("value").get(value).contains(resoutce3));
				assertEquals(2, index2.get("value").get(value).size());
				break;
			default:
				fail("");
			}
		}
	}

	@Test
	public void getResourceKeys() {
		// TODO
	}
}
