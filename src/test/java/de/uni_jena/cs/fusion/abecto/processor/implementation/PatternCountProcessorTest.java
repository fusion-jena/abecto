package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_NAME;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.CATEGORY_TARGET;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.COUNT_MEASURE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.KNOWLEDGE_BASE;
import static de.uni_jena.cs.fusion.abecto.util.Vocabulary.VALUE;

import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
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
		UUID sourceUUID1 = UUID.randomUUID();
		UUID sourceUUID2 = UUID.randomUUID();
		source1.setKnowledgBase(sourceUUID1);
		source2.setKnowledgBase(sourceUUID2);
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

		Model resultModel = countProcessor.getResultModel();
		ExprFactory e = new ExprFactory();
		Node measureVar = NodeFactory.createVariable("measure");
		Node knowledgebaseVar = NodeFactory.createVariable("knowledgebase");
		Node categoryVar = NodeFactory.createVariable("category");
		Node targetVar = NodeFactory.createVariable("target");
		Node countVar = NodeFactory.createVariable("count");

		SelectBuilder selectBuilder = new SelectBuilder().addVar(countVar).addWhere(measureVar, RDF.type, COUNT_MEASURE)
				.addWhere(measureVar, KNOWLEDGE_BASE, knowledgebaseVar).addWhere(measureVar, CATEGORY_NAME, categoryVar)
				.addWhere(measureVar, VALUE, countVar)
				.addFilter(e.notexists(new SelectBuilder().addWhere(measureVar, CATEGORY_TARGET.asNode(), targetVar)));

		Literal sourceLiteral1 = ResourceFactory.createStringLiteral(sourceUUID1.toString());
		Literal sourceLiteral2 = ResourceFactory.createStringLiteral(sourceUUID2.toString());
		Literal categoryLiteral = ResourceFactory.createStringLiteral(generator.generateClassName(0));
		Literal objectPropertyLiteral = ResourceFactory.createStringLiteral(generator.generateObjectPropertyName(0));
		Literal dataPropertyLiteral = ResourceFactory.createStringLiteral(generator.generateDataPropertyName(0));

		// result checks
		selectBuilder.setVar(knowledgebaseVar, sourceLiteral1);
		selectBuilder.setVar(categoryVar, categoryLiteral);
		Assertions.assertEquals(100L, getFirst(selectBuilder, resultModel));

		selectBuilder.setVar(knowledgebaseVar, sourceLiteral2);
		Assertions.assertEquals(100L, getFirst(selectBuilder, resultModel));

		selectBuilder = new SelectBuilder().addVar(countVar).addWhere(measureVar, RDF.type, COUNT_MEASURE)
				.addWhere(measureVar, KNOWLEDGE_BASE, knowledgebaseVar).addWhere(measureVar, CATEGORY_NAME, categoryVar)
				.addWhere(measureVar, CATEGORY_TARGET, targetVar).addWhere(measureVar, VALUE, countVar)
				.addValueVar(knowledgebaseVar).addValueVar(categoryVar).addValueVar(targetVar);

		selectBuilder.setVar(knowledgebaseVar, sourceLiteral1);
		selectBuilder.setVar(categoryVar, categoryLiteral);
		selectBuilder.setVar(targetVar, objectPropertyLiteral);
		Assertions.assertEquals(98L, getFirst(selectBuilder, resultModel));

		selectBuilder.setVar(targetVar, dataPropertyLiteral);
		Assertions.assertEquals(98L, getFirst(selectBuilder, resultModel));

		selectBuilder.setVar(knowledgebaseVar, sourceLiteral2);
		selectBuilder.setVar(targetVar, objectPropertyLiteral);
		Assertions.assertEquals(95L, getFirst(selectBuilder, resultModel));

		selectBuilder.setVar(targetVar, dataPropertyLiteral);
		Assertions.assertEquals(95L, getFirst(selectBuilder, resultModel));

	}

	private long getFirst(SelectBuilder selectBuilder, Model model) {
		selectBuilder.buildString();
		return QueryExecutionFactory.create(selectBuilder.build(), model).execSelect().next().get("count").asLiteral()
				.getLong();
	}

}
