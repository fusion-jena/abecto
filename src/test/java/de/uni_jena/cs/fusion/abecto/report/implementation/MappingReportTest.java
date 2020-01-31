package de.uni_jena.cs.fusion.abecto.report.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.Processor.Status;
import de.uni_jena.cs.fusion.abecto.processor.implementation.JaroWinklerMappingProcessor;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.Mapping;
import de.uni_jena.cs.fusion.abecto.processor.model.PositiveMapping;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class MappingReportTest {
	@Test
	void of() throws Exception {
		// preparation
		Model FIRST_GRAPH = Models.load(new ByteArrayInputStream(("" + //
				"@base <http://example.org/> .\r\n" + //
				"@prefix : <http://example.org/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghefgh\" .\r\n" + //
				":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		Model SECOND_GRAPH = Models.load(new ByteArrayInputStream(("" + //
				"@base <http://example.com/> .\r\n" + //
				"@prefix : <http://example.com/> .\r\n" + //
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + //
				"\r\n" + //
				":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\r\n" + //
				":entity2 rdfs:label \"efghefghefghefghabcd\" .\r\n" + //
				":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		Model META_GRAPH = Models.getEmptyOntModel();
		SparqlEntityManager.insert(new Category("entity",
				"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .", UUID.randomUUID()), META_GRAPH);

		JaroWinklerMappingProcessor processor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		processor.setParameters(parameter);
		processor.addInputModelGroups(Map.of(UUID.randomUUID(), Collections.singleton(FIRST_GRAPH), UUID.randomUUID(),
				Collections.singleton(SECOND_GRAPH)));
		processor.addMetaModels(Collections.singleton(META_GRAPH));
		Model mapping = processor.computeResultModel();
		processor.setResultModel(mapping);
		processor.setStatus(Status.SUCCEEDED);

		// test
		@SuppressWarnings("unchecked")
		Collection<PositiveMapping> report = (Collection<PositiveMapping>) new MappingReport().of(processor);

		assertEquals(2, report.size());
		assertTrue(report.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity1"),
				ResourceFactory.createResource("http://example.com/entity1"))));
		assertTrue(report.contains(Mapping.of(ResourceFactory.createResource("http://example.org/entity2"),
				ResourceFactory.createResource("http://example.com/entity2"))));

	}

}
