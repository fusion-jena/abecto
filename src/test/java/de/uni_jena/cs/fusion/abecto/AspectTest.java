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
package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;

public class AspectTest {

	@BeforeAll
	public static void init() {
		Abecto.initApacheJena();
	}

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

		Collection<Aspect> aspects = Aspect.getAspects(configurationModel);
		aspects.stream().anyMatch(aspect -> aspect1Resource.equals(aspect.getIri()));
		aspects.stream().anyMatch(aspect -> aspect2Resource.equals(aspect.getIri()));
		for (Aspect aspect : aspects) {
			if (aspect1Resource.equals(aspect.getIri())) {
				assertEquals("key", aspect.getKeyVariableName());
				assertEquals(Var.alloc("key"), aspect.getKeyVariable());
				assertEquals(pattern1, aspect.getPattern(dataset1));
				assertEquals(pattern2, aspect.getPattern(dataset2));
			}
			if (aspect2Resource.equals(aspect.getIri())) {
				assertEquals("key", aspect.getKeyVariableName());
				assertEquals(Var.alloc("key"), aspect.getKeyVariable());
				assertEquals(pattern3, aspect.getPattern(dataset1));
				assertEquals(pattern4, aspect.getPattern(dataset2));
			}
		}
	}

	@Test
	public void getResource() {
		Model primaryDataModel = ModelFactory.createDefaultModel();
		Property property1 = ResourceFactory.createProperty("http://example.org/property1");
		Property property2 = ResourceFactory.createProperty("http://example.org/property2");
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		Resource resource1 = ResourceFactory.createResource("http://example.org/1");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2");
		Resource resource3 = ResourceFactory.createResource("http://example.org/3");
		Resource resource4 = ResourceFactory.createResource("http://example.org/4");
		primaryDataModel.addLiteral(resource1, property1, 1);
		primaryDataModel.addLiteral(resource2, property1, 2);
		primaryDataModel.addLiteral(resource3, property1, 3);
		primaryDataModel.addLiteral(resource4, property1, 4);

		primaryDataModel.addLiteral(resource1, property2, 1);
		primaryDataModel.addLiteral(resource1, property2, 2);
		primaryDataModel.addLiteral(resource1, property2, 3);

		primaryDataModel.addLiteral(resource2, property2, 2);
		primaryDataModel.addLiteral(resource2, property2, 3);
		primaryDataModel.addLiteral(resource2, property2, 4);

		primaryDataModel.addLiteral(resource3, property2, 3);
		primaryDataModel.addLiteral(resource3, property2, 4);
		primaryDataModel.addLiteral(resource3, property2, 5);

		primaryDataModel.addLiteral(resource4, property2, 4);
		primaryDataModel.addLiteral(resource4, property2, 5);
		primaryDataModel.addLiteral(resource4, property2, 6);
		Aspect aspect = new Aspect(aspectIri, "key");
		Query pattern = QueryFactory.create("SELECT ?key ?value1 ?value2 WHERE {?key <" + property1.getURI()
				+ "> ?value1 ; <" + property2.getURI() + "> ?value2 .}");
		aspect.setPattern(dataset, pattern);

		for (Resource key : Arrays.asList(resource1, resource2, resource3, resource4)) {
			int resourceId = Integer.parseInt(key.getURI().substring("http://example.org/".length()));
			int[] expectedValues1 = new int[] { resourceId };
			int[] expectedValues2 = new int[] { resourceId, resourceId + 1, resourceId + 2 };
			Map<String, Set<RDFNode>> resourceValues = Aspect.getResource(aspect, dataset, key, primaryDataModel)
					.orElseThrow(() -> new AssertionFailedError(
							String.format("Failed to get resource \"%s\".", key.getURI())));
			assertArrayEquals(expectedValues1,
					resourceValues.get("value1").stream().mapToInt(l -> l.asLiteral().getInt()).sorted().toArray(),
					() -> String.format("Unexpected values for resource \"%s\" and variable \"%s\".", key.getURI(),
							"value1"));
			assertArrayEquals(expectedValues2,
					resourceValues.get("value2").stream().mapToInt(l -> l.asLiteral().getInt()).sorted().toArray(),
					() -> String.format("Unexpected values for resource \"%s\" and variable \"%s\".", key.getURI(),
							"value1"));
		}

		assertFalse(Aspect.getResource(aspect, dataset, aspectIri, primaryDataModel).isPresent());
	}

	@Test
	public void getResourceIndex() {
		Model primaryDataModel = ModelFactory.createDefaultModel();
		Property property = ResourceFactory.createProperty("http://example.org/property");
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		Resource resource1 = ResourceFactory.createResource("http://example.org/1");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2");
		Resource resource3 = ResourceFactory.createResource("http://example.org/3");
		Resource resource4 = ResourceFactory.createResource("http://example.org/4");
		primaryDataModel.addLiteral(resource1, property, 1);
		primaryDataModel.addLiteral(resource2, property, 2);
		primaryDataModel.addLiteral(resource3, property, 3);
		primaryDataModel.addLiteral(resource4, property, 4);
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
				assertTrue(index1.get("value").get(value).contains(resource1));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 2:
				assertTrue(index1.get("value").get(value).contains(resource2));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 3:
				assertTrue(index1.get("value").get(value).contains(resource3));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			case 4:
				assertTrue(index1.get("value").get(value).contains(resource4));
				assertEquals(1, index1.get("value").get(value).size());
				break;
			default:
				fail("Unexpected value.");
			}
		}

		// with modifier

		Map<String, Map<Integer, Set<Resource>>> index2 = Aspect.getResourceIndex(aspect, dataset,
				Collections.singleton("value"), primaryDataModel, l -> (l.asLiteral().getInt() % 2));

		assertTrue(index2.get("value").keySet().contains(0));
		assertTrue(index2.get("value").keySet().contains(1));

		for (Integer value : index2.get("value").keySet()) {
			switch (value.intValue()) {
			case 0:
				assertTrue(index2.get("value").get(value).contains(resource2));
				assertTrue(index2.get("value").get(value).contains(resource4));
				assertEquals(2, index2.get("value").get(value).size());
				break;
			case 1:
				assertTrue(index2.get("value").get(value).contains(resource1));
				assertTrue(index2.get("value").get(value).contains(resource3));
				assertEquals(2, index2.get("value").get(value).size());
				break;
			default:
				fail("Unexpected value.");
			}
		}
	}

	@Test
	public void getResourceKeys() {
		Model primaryDataModel = ModelFactory.createDefaultModel();
		Property property = ResourceFactory.createProperty("http://example.org/property");
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		Resource resource1 = ResourceFactory.createResource("http://example.org/1");
		Resource resource2 = ResourceFactory.createResource("http://example.org/2");
		Resource resource3 = ResourceFactory.createResource("http://example.org/3");
		Resource resource4 = ResourceFactory.createResource("http://example.org/4");
		primaryDataModel.addLiteral(resource1, property, 1);
		primaryDataModel.addLiteral(resource2, property, 2);
		primaryDataModel.addLiteral(resource3, property, 3);
		primaryDataModel.addLiteral(resource4, property, 4);
		Aspect aspect = new Aspect(aspectIri, "key");
		Query pattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property.getURI() + "> ?value .}");
		aspect.setPattern(dataset, pattern);

		Set<Resource> resources = Aspect.getResourceKeys(aspect, dataset, primaryDataModel).collect(Collectors.toSet()	);

		assertEquals(4, resources.size());
		assertTrue(resources.contains(resource1));
		assertTrue(resources.contains(resource2));
		assertTrue(resources.contains(resource3));
		assertTrue(resources.contains(resource4));
	}

	@Test
	public void getVarPaths() {
		Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
		Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
		Aspect aspect = new Aspect(aspectIri, "key");
		aspect.setPattern(dataset, QueryFactory.create(""//
				+ "PREFIX ex: <http://example.org/>"//
				+ "SELECT ?key ?value WHERE {"//
				+ "?key ex:p0 ?v0 ."//
				+ "?v0 ex:p1/ex:p2 ?v012 ."//
				+ "?v012 ^ex:p3 ?v012i3 ."//
				+ "?v012i3 ex:p4 [ ex:p5 ?v012i345 ; ^ex:p5 ?v012i34i5 ] ."//
				+ "{?v0 ex:p8a ?v08 .}"//
				+ "UNION"//
				+ "{?v0 ex:p8b ?v08 .}"//
				+ "}"));

		Model model = ModelFactory.createDefaultModel();
		model.createResource(AV.AspectPattern).addProperty(AV.ofAspect, aspectIri).addProperty(AV.associatedDataset,
				dataset);

		aspect.determineVarPaths(model);

		assertEquals("<http://example.org/p0>", getVarPath(model, aspectIri, dataset, "v0"));
		assertEquals("<http://example.org/p0>/(<http://example.org/p1>/<http://example.org/p2>)",
				getVarPath(model, aspectIri, dataset, "v012"));
		assertEquals(
				"<http://example.org/p0>/(<http://example.org/p1>/(<http://example.org/p2>/^<http://example.org/p3>))",
				getVarPath(model, aspectIri, dataset, "v012i3"));
		assertEquals(
				"<http://example.org/p0>/(<http://example.org/p1>/(<http://example.org/p2>/(^<http://example.org/p3>/(<http://example.org/p4>/<http://example.org/p5>))))",
				getVarPath(model, aspectIri, dataset, "v012i345"));
		assertEquals(
				"<http://example.org/p0>/(<http://example.org/p1>/(<http://example.org/p2>/(^<http://example.org/p3>/(<http://example.org/p4>/^<http://example.org/p5>))))",
				getVarPath(model, aspectIri, dataset, "v012i34i5"));
		assertEquals("<http://example.org/p0>/(<http://example.org/p8a>|<http://example.org/p8b>)",
				getVarPath(model, aspectIri, dataset, "v08"));

	}

	private String getVarPath(Model model, Resource aspectIri, Resource dataset, String variableName) {
		return QueryExecutionFactory.create(""//
				+ "SELECT ?path WHERE {["//
				+ "<" + AV.ofAspect.getURI() + "> <" + aspectIri.getURI() + "> ;"//
				+ "<" + AV.associatedDataset.getURI() + "> <" + dataset.getURI() + "> ;"//
				+ "<" + AV.hasVariablePath.getURI() + "> ["//
				+ "<" + AV.variableName.getURI() + "> \"" + variableName + "\" ;"//
				+ "<" + AV.propertyPath.getURI() + "> ?path ;"//
				+ "]]}", model).execSelect().next().get("path").asLiteral().getLexicalForm();
	}

	/**
	 * For https://issues.apache.org/jira/browse/JENA-2335
	 */
	@Test
	public void retainVariables() {
		String queryStr = "SELECT ?a ?d WHERE { ?a <http://example.org/p> ?b . BIND(?b AS ?c) BIND(?c AS ?d) }";
		String queryStr2 = Aspect.retainVariables(QueryFactory.create(queryStr), Var.alloc("a"), Arrays.asList("d"))
				.toString();
		assertTrue(queryStr2.contains("(?b AS ?c)"));

	}
}
