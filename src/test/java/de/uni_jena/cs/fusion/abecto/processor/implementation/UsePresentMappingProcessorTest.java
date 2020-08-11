package de.uni_jena.cs.fusion.abecto.processor.implementation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.metaentity.Issue;
import de.uni_jena.cs.fusion.abecto.metaentity.Mapping;
import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class UsePresentMappingProcessorTest {

	@Test
	public void computeMapping() throws Exception {
		UsePresentMappingProcessor.Parameter parameter = new UsePresentMappingProcessor.Parameter();
		parameter.assignmentPaths.add("<http://example.org/sameAs>");
		parameter.assignmentPaths.add("<http://example.org/same>/<http://example.org/as>");

		String inputRdf = "<http://example.org/a1> <http://example.org/sameAs> <http://example.org/a2> ."
				+ "<http://example.org/b1> <http://example.org/same> <http://example.org/b-link> ."
				+ "<http://example.org/b-link> <http://example.org/as> <http://example.org/b2> ."
				+ "<http://example.org/c1> <http://example.org/sameAs> <http://example.org/c2> ."
				+ "<http://example.org/d1> <http://example.org/sameAs> <http://example.org/d2> ."
				+ "<http://example.org/e1> <http://example.org/sameAs> \"issue\" .";
		Model inputModel = Models.read(new ByteArrayInputStream(inputRdf.getBytes()));

		Model inputMetaModel = Models.getEmptyOntModel();
		SparqlEntityManager.insert(Arrays.asList(
				Mapping.of(ResourceFactory.createResource("http://example.org/c1"),
						ResourceFactory.createResource("http://example.org/c2")),
				Mapping.not(ResourceFactory.createResource("http://example.org/d1"),
						ResourceFactory.createResource("http://example.org/d2"))),
				inputMetaModel);

		UsePresentMappingProcessor processor = new UsePresentMappingProcessor();
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(inputModel));
		processor.addInputModelGroup(UUID.randomUUID(), Collections.singleton(Models.getEmptyModel()));
		processor.addMetaModels(Collections.singleton(inputMetaModel));
		processor.setParameters(parameter);
		Model outputModel = processor.call();

		Collection<Mapping> mappings = SparqlEntityManager.select(Mapping.any(), outputModel);
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/a1"),
				ResourceFactory.createResource("http://example.org/a2"))));
		assertTrue(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/b1"),
				ResourceFactory.createResource("http://example.org/b2"))));
		assertFalse(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/c1"),
				ResourceFactory.createResource("http://example.org/c2"))));
		assertFalse(mappings.contains(Mapping.of(ResourceFactory.createResource("http://example.org/d1"),
				ResourceFactory.createResource("http://example.org/d2"))));

		Collection<Issue> issues = SparqlEntityManager.select(new Issue(), outputModel);
		assertFalse(issues.isEmpty());
	}
}
