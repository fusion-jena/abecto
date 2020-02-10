package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.TestDataGenerator;
import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class ManualCategoryProcessorTest {

	private static RdfFileSourceProcessor source;

	@BeforeAll
	public static void init() throws Exception {
		source = new RdfFileSourceProcessor();
		source.setUploadStream(new TestDataGenerator().stream(1));
		source.setKnowledgBase(UUID.randomUUID());
		source.call();
	}

	@Test
	public void computeResultModel() throws Exception {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put("good", "?good <" + RDFS.subClassOf + "> <" + OWL.Thing + ">");
		parameter.patterns.put("bad", "?bad <" + RDF.type + "> <" + RDFS.Class + ">");
		processor.setParameters(parameter);
		processor.computeResultModel();
		Model model = processor.getResultModel();
		Assertions.assertEquals(2, model.listResourcesWithProperty(RDF.type, Vocabulary.CATEGORY).toSet().size());

	}

	@Test
	public void invalidTemplate() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put("test", "");
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyPatternList() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		processor.addInputProcessor(source);
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}
}
