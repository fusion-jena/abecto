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

import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.metaentity.Measurement;
import de.uni_jena.cs.fusion.abecto.processor.RdfFileSourceProcessor;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class CategoryCountProcessorTest {

	@Test
	public void test() throws Exception {
		// generate data
		TestDataGenerator generator = new TestDataGenerator().setClassFactor(1).setObjectPropertyFactor(1)
				.setDataPropertyFactor(1).setIndividualFactor(100).setDensity(1);
		InputStream model1 = generator.setErrorRate(20).setGapRate(50).stream(1);
		InputStream model2 = generator.setErrorRate(50).setGapRate(20).stream(2);
		// load data
		RdfFileSourceProcessor source1 = new RdfFileSourceProcessor();
		RdfFileSourceProcessor source2 = new RdfFileSourceProcessor();
		source1.setUploadStream(model1);
		source2.setUploadStream(model2);
		UUID sourceUUID1 = UUID.randomUUID();
		UUID sourceUUID2 = UUID.randomUUID();
		source1.setOntology(sourceUUID1);
		source2.setOntology(sourceUUID2);
		source1.call();
		source2.call();

		// generate and load patterns
		ManualCategoryProcessor patternProcessor1 = new ManualCategoryProcessor();
		ManualCategoryProcessor patternProcessor2 = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter patternParameter1 = new ManualCategoryProcessor.Parameter();
		ManualCategoryProcessor.Parameter patternParameter2 = new ManualCategoryProcessor.Parameter();
		patternParameter1.patterns = generator.generatePatterns(1);
		patternProcessor1.setParameters(patternParameter1);
		patternProcessor1.addInputProcessor(source1);
		patternProcessor1.call();
		patternParameter2.patterns = generator.generatePatterns(2);
		patternProcessor2.setParameters(patternParameter2);
		patternProcessor2.addInputProcessor(source2);
		patternProcessor2.call();

		// generate counts
		CategoryCountProcessor countProcessor = new CategoryCountProcessor();
		countProcessor.addInputModelGroups(patternProcessor1.getDataModels());
		countProcessor.addInputModelGroups(patternProcessor2.getDataModels());
		countProcessor.addMetaModels(patternProcessor1.getMetaModels());
		countProcessor.addMetaModels(patternProcessor2.getMetaModels());
		countProcessor.call();

		// check results
		Model resultModel = countProcessor.getResultModel();
		String categoryName = generator.generateClassName(0);
		String objectPropertyName = generator.generateObjectPropertyName(0);
		String dataPropertyName = generator.generateDataPropertyName(0);
		Measurement measurement;

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID1, categoryName, null, null), resultModel)
				.orElseThrow();
		Assertions.assertEquals(100L, measurement.value);

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID1, categoryName, objectPropertyName, null),
						resultModel)
				.orElseThrow();
		Assertions.assertEquals(98L, measurement.value);

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID1, categoryName, dataPropertyName, null),
						resultModel)
				.orElseThrow();
		Assertions.assertEquals(98L, measurement.value);

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID2, categoryName, null, null), resultModel)
				.orElseThrow();
		Assertions.assertEquals(100L, measurement.value);

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID2, categoryName, objectPropertyName, null),
						resultModel)
				.orElseThrow();
		Assertions.assertEquals(95L, measurement.value);

		measurement = SparqlEntityManager
				.selectOne(CategoryCountProcessor.measurement(sourceUUID2, categoryName, dataPropertyName, null),
						resultModel)
				.orElseThrow();
		Assertions.assertEquals(95L, measurement.value);
	}

}
