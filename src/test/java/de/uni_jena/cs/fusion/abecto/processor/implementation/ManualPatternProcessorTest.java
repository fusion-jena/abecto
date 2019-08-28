package de.uni_jena.cs.fusion.abecto.processor.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.util.Vocabulary;

public class ManualPatternProcessorTest {

	@Test
	public void computeResultModel() throws Exception {
		ManualPatternProcessor processor = new ManualPatternProcessor();
		ManualPatternProcessor.Parameter parameter = new ManualPatternProcessor.Parameter();
		parameter.patterns = new HashMap<>();
		parameter.patterns.put("good", Arrays.asList("?good <" + RDF.type + "> <" + RDFS.Class + ">",
				"?good <" + RDFS.subClassOf + "> <" + OWL.Thing + ">"));
		parameter.patterns.put("bad", Arrays.asList("?bad <" + RDF.type + "> <" + RDFS.Class + ">",
				"?bad <" + RDFS.subClassOf + "> <" + OWL.Thing + ">"));
		processor.setParameters(parameter);
		Model model = processor.computeResultModel();
		Assertions.assertEquals(4, model.listSubjectsWithProperty(RDF.type, Vocabulary.CATEGORY).toSet().size());
	}

	@Test
	public void invalidTemplate() {
		ManualPatternProcessor processor = new ManualPatternProcessor();
		ManualPatternProcessor.Parameter parameter = new ManualPatternProcessor.Parameter();
		parameter.patterns = new HashMap<>();
		parameter.patterns.put("test", Collections.singletonList(""));
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyCategoryList() {
		ManualPatternProcessor processor = new ManualPatternProcessor();
		ManualPatternProcessor.Parameter parameter = new ManualPatternProcessor.Parameter();
		parameter.patterns = new HashMap<>();
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}

	@Test
	public void emptyTemplateList() {
		ManualPatternProcessor processor = new ManualPatternProcessor();
		ManualPatternProcessor.Parameter parameter = new ManualPatternProcessor.Parameter();
		parameter.patterns = new HashMap<>();
		parameter.patterns.put("test", Collections.emptyList());
		processor.setParameters(parameter);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			processor.computeResultModel();
		});
	}
}
