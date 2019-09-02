package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.io.InputStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;

public class PatternCountProcessorTest {

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
		source1.setKnowledgBase(UUID.randomUUID());
		source2.setKnowledgBase(UUID.randomUUID());
		source1.call();
		source2.call();

		// generate and load patterns
		ManualPatternProcessor patternProcessor1 = new ManualPatternProcessor();
		ManualPatternProcessor patternProcessor2 = new ManualPatternProcessor();
		ManualPatternProcessor.Parameter patternParameter1 = new ManualPatternProcessor.Parameter();
		ManualPatternProcessor.Parameter patternParameter2 = new ManualPatternProcessor.Parameter();
		patternParameter1.patterns = generator.generatePatterns(1);
		patternParameter2.patterns = generator.generatePatterns(2);
		patternProcessor1.setParameters(patternParameter1);
		patternProcessor2.setParameters(patternParameter2);
		patternProcessor1.addInputModelGroups(source1.getDataModels());
		patternProcessor1.addInputModelGroups(source2.getDataModels());
		patternProcessor1.call();
		patternProcessor2.addInputModelGroups(patternProcessor1.getDataModels());
		patternProcessor2.addMetaModels(patternProcessor1.getMetaModel());
		patternProcessor2.call();

		// generate counts
		PatternCountProcessor countProcessor = new PatternCountProcessor();
		countProcessor.addInputModelGroups(patternProcessor2.getDataModels());
		countProcessor.addMetaModels(patternProcessor2.getMetaModel());
		countProcessor.call().write(System.out, "JSON-LD");
		
		// TODO add result checks
	}

}
