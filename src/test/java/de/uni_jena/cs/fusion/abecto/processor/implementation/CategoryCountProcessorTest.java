package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.processor.model.CategoryCountMeasure;
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
		source1.setKnowledgBase(sourceUUID1);
		source2.setKnowledgBase(sourceUUID2);
		source1.call();
		source2.call();

		// generate and load patterns
		ManualCategoryProcessor patternProcessor1 = new ManualCategoryProcessor();
		ManualCategoryProcessor patternProcessor2 = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter patternParameter1 = new ManualCategoryProcessor.Parameter();
		ManualCategoryProcessor.Parameter patternParameter2 = new ManualCategoryProcessor.Parameter();
		patternParameter1.patterns = Collections.singletonMap(sourceUUID1, generator.generatePatterns(1));
		patternParameter2.patterns = Collections.singletonMap(sourceUUID2, generator.generatePatterns(2));
		patternProcessor1.setParameters(patternParameter1);
		patternProcessor2.setParameters(patternParameter2);
		patternProcessor1.addInputModelGroups(source1.getDataModels());
		patternProcessor1.addInputModelGroups(source2.getDataModels());
		patternProcessor1.call();
		patternProcessor2.addInputModelGroups(patternProcessor1.getDataModels());
		patternProcessor2.addMetaModels(patternProcessor1.getMetaModel());
		patternProcessor2.call();

		// generate counts
		CategoryCountProcessor countProcessor = new CategoryCountProcessor();
		countProcessor.addInputModelGroups(patternProcessor2.getDataModels());
		countProcessor.addMetaModels(patternProcessor2.getMetaModel());
		countProcessor.call();

		// check results
		Model resultModel = countProcessor.getResultModel();
		String categoryName = generator.generateClassName(0);
		String objectPropertyName = generator.generateObjectPropertyName(0);
		String dataPropertyName = generator.generateDataPropertyName(0);
		CategoryCountMeasure measure;

		measure = SparqlEntityManager.selectOne(new CategoryCountMeasure(categoryName, Optional.empty(), null, sourceUUID1),
				resultModel);
		Assertions.assertEquals(100L, measure.value);

		measure = SparqlEntityManager
				.selectOne(new CategoryCountMeasure(categoryName, Optional.of(objectPropertyName), null, sourceUUID1), resultModel);
		Assertions.assertEquals(98L, measure.value);

		measure = SparqlEntityManager
				.selectOne(new CategoryCountMeasure(categoryName, Optional.of(dataPropertyName), null, sourceUUID1), resultModel);
		Assertions.assertEquals(98L, measure.value);

		measure = SparqlEntityManager.selectOne(new CategoryCountMeasure(categoryName, Optional.empty(), null, sourceUUID2),
				resultModel);
		Assertions.assertEquals(100L, measure.value);

		measure = SparqlEntityManager
				.selectOne(new CategoryCountMeasure(categoryName, Optional.of(objectPropertyName), null, sourceUUID2), resultModel);
		Assertions.assertEquals(95L, measure.value);

		measure = SparqlEntityManager
				.selectOne(new CategoryCountMeasure(categoryName, Optional.of(dataPropertyName), null, sourceUUID2), resultModel);
		Assertions.assertEquals(95L, measure.value);
	}

}
