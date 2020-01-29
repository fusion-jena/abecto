package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class ManualCategoryProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		Map<String, String> patternByName = new HashMap<>();
		parameter.patterns.put(UUID.randomUUID(), patternByName);
		patternByName.put("good", "?good <" + RDFS.subClassOf + "> <" + OWL.Thing + ">");
		patternByName.put("bad", "?bad <" + RDF.type + "> <" + RDFS.Class + ">");
		processor.setParameters(parameter);
		Model model = processor.computeResultModel();
		Assertions.assertEquals(2, model.listResourcesWithProperty(RDF.type, Vocabulary.CATEGORY).toSet().size());
	}

	@Test
	public void invalidTemplate() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		Map<String, String> patternByName = new HashMap<>();
		parameter.patterns.put(UUID.randomUUID(), patternByName);
		patternByName.put("test", "");
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyCategoryList() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns = new HashMap<>();
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyPatternList() {
		ManualCategoryProcessor processor = new ManualCategoryProcessor();
		ManualCategoryProcessor.Parameter parameter = new ManualCategoryProcessor.Parameter();
		parameter.patterns.put(UUID.randomUUID(), new HashMap<>());
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}
}
