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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.vocabulary.AV;
import de.uni_jena.cs.fusion.abecto.vocabulary.PPlan;

public class PlansTest {
	@Test
	public void getPlan() {
		Model configurationModel = ModelFactory.createDefaultModel();
		String plan1Iri = "http://example.org/plan1";
		Resource plan1 = ResourceFactory.createResource(plan1Iri);
		String plan2Iri = "http://example.org/plan2";
		Resource plan2 = ResourceFactory.createResource(plan2Iri);

		assertThrows(IllegalArgumentException.class, () -> Plans.getPlan(configurationModel, null));
		assertThrows(IllegalArgumentException.class, () -> Plans.getPlan(configurationModel, plan1Iri));

		configurationModel.createResource(plan1Iri, AV.Plan);

		assertEquals(plan1, Plans.getPlan(configurationModel, null));
		assertEquals(plan1, Plans.getPlan(configurationModel, plan1Iri));
		assertThrows(IllegalArgumentException.class, () -> Plans.getPlan(configurationModel, plan2Iri));

		configurationModel.createResource(plan2Iri, AV.Plan);

		assertThrows(IllegalArgumentException.class, () -> Plans.getPlan(configurationModel, null));
		assertEquals(plan1, Plans.getPlan(configurationModel, plan1Iri));
		assertEquals(plan2, Plans.getPlan(configurationModel, plan2Iri));
	}

	@Test
	public void getStepPredecessors() {
		Model configurationModel = ModelFactory.createDefaultModel();
		Resource plan1 = ResourceFactory.createResource("http://example.org/plan1");
		Resource plan2 = ResourceFactory.createResource("http://example.org/plan2");
		Resource step1 = ResourceFactory.createResource("http://example.org/step1");
		Resource step2 = ResourceFactory.createResource("http://example.org/step2");
		Resource step3 = ResourceFactory.createResource("http://example.org/step3");
		Resource step4 = ResourceFactory.createResource("http://example.org/step4");
		Resource step5 = ResourceFactory.createResource("http://example.org/step5");
		Resource step6 = ResourceFactory.createResource("http://example.org/step6");
		configurationModel.add(step1, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step2, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step3, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step4, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step5, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step6, PPlan.isStepOfPlan, plan1);
		configurationModel.add(step2, PPlan.isPrecededBy, step1);
		configurationModel.add(step4, PPlan.isPrecededBy, step3);
		configurationModel.add(step5, PPlan.isPrecededBy, step4);
		configurationModel.add(step5, PPlan.isPrecededBy, step2);
		configurationModel.add(step6, PPlan.isPrecededBy, step5);

		Map<Resource, Set<Resource>> stepPredecessors = Plans.getStepPredecessors(configurationModel, plan1);

		assertFalse(stepPredecessors.isEmpty());
		assertArrayEquals(new Resource[] {}, stepPredecessors.get(step1).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));
		assertArrayEquals(new Resource[] { step1 }, stepPredecessors.get(step2).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));
		assertArrayEquals(new Resource[] {}, stepPredecessors.get(step3).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));
		assertArrayEquals(new Resource[] { step3 }, stepPredecessors.get(step4).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));
		assertArrayEquals(new Resource[] { step1, step2, step3, step4 }, stepPredecessors.get(step5).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));
		assertArrayEquals(new Resource[] { step1, step2, step3, step4, step5 }, stepPredecessors.get(step6).stream()
				.sorted(Comparator.comparing(Resource::getURI)).toArray(Resource[]::new));

		assertTrue(Plans.getStepPredecessors(configurationModel, plan2).isEmpty());

	}
}
