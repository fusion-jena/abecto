package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.processor.Processor.Status;
import de.uni_jena.cs.fusion.abecto.processor.model.Category;
import de.uni_jena.cs.fusion.abecto.processor.model.MappingReportEntity;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

class MappingReportProcessorTest {
	@Test
	void test() throws Exception {
		// preparation
		Model FIRST_GRAPH = Models.load(new ByteArrayInputStream((""//
				+ "@base <http://example.org/> .\n"//
				+ "@prefix : <http://example.org/> .\n"//
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"//
				+ ":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\n"//
				+ ":entity2 rdfs:label \"efghefghefghefghefgh\" .\n"//
				+ ":entity3 rdfs:label \"ijklijklijklijklijkl\" .").getBytes()));
		Model SECOND_GRAPH = Models.load(new ByteArrayInputStream((""//
				+ "@base <http://example.com/> .\n"//
				+ "@prefix : <http://example.com/> .\n"//
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"//
				+ ":entity1 rdfs:label \"abcdabcdabcdabcdabcd\" .\n"//
				+ ":entity2 rdfs:label \"efghefghefghefghabcd\" .\n"//
				+ ":entity3 rdfs:label \"mnopmnopmnopmnopmnop\" .").getBytes()));
		Model META_GRAPH = Models.getEmptyOntModel();
		UUID FIRST_UUID = UUID.randomUUID();
		UUID SECOND_UUID = UUID.randomUUID();
		SparqlEntityManager.insert(
				new Category("entity", "?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .", FIRST_UUID),
				META_GRAPH);
		SparqlEntityManager.insert(
				new Category("entity", "?entity <http://www.w3.org/2000/01/rdf-schema#label> ?label .", SECOND_UUID),
				META_GRAPH);

		JaroWinklerMappingProcessor mappingProcessor = new JaroWinklerMappingProcessor();
		JaroWinklerMappingProcessor.Parameter parameter = new JaroWinklerMappingProcessor.Parameter();
		parameter.case_sensitive = false;
		parameter.threshold = 0.90D;
		parameter.category = "entity";
		parameter.variables = Collections.singleton("label");
		mappingProcessor.setParameters(parameter);
		mappingProcessor.addInputModelGroups(Map.of(FIRST_UUID, Collections.singleton(FIRST_GRAPH), SECOND_UUID,
				Collections.singleton(SECOND_GRAPH)));
		mappingProcessor.addMetaModels(Collections.singleton(META_GRAPH));
		Model mapping = mappingProcessor.call();
		mappingProcessor.setStatus(Status.SUCCEEDED, mapping);

		// test
		MappingReportProcessor reportProcessor = new MappingReportProcessor();
		reportProcessor.addMetaModels(mappingProcessor.getMetaModels());
		reportProcessor.addInputModelGroups(mappingProcessor.getDataModels());
		Model report = reportProcessor.call();
		Collection<MappingReportEntity> reportEntities = SparqlEntityManager.select(MappingReportEntity.ALL, report);

		assertEquals(4, reportEntities.size());
		assertTrue(reportEntities.stream().anyMatch((mre) -> {
			return mre.first.isPresent() && mre.first.get().equals("http://example.org/entity1")
					&& mre.second.isPresent() && mre.second.get().equals("http://example.com/entity1")
					|| mre.first.isPresent() && mre.first.get().equals("http://example.com/entity1")
							&& mre.second.isPresent() && mre.second.get().equals("http://example.org/entity1");
		}));
		assertTrue(reportEntities.stream().anyMatch((mre) -> {
			return mre.first.isPresent() && mre.first.get().equals("http://example.org/entity2")
					&& mre.second.isPresent() && mre.second.get().equals("http://example.com/entity2")
					|| mre.first.isPresent() && mre.first.get().equals("http://example.com/entity2")
							&& mre.second.isPresent() && mre.second.get().equals("http://example.org/entity2");
		}));
		assertTrue(reportEntities.stream().anyMatch((mre) -> {
			return mre.first.isPresent() && mre.first.get().equals("http://example.org/entity3") && mre.second.isEmpty()
					|| mre.first.isEmpty() && mre.second.isPresent()
							&& mre.second.get().equals("http://example.org/entity3");
		}));
		assertTrue(reportEntities.stream().anyMatch((mre) -> {
			return mre.first.isEmpty() && mre.second.isPresent()
					&& mre.second.get().equals("http://example.com/entity3")
					|| mre.first.isPresent() && mre.first.get().equals("http://example.com/entity3")
							&& mre.second.isEmpty();
		}));
	}

}
