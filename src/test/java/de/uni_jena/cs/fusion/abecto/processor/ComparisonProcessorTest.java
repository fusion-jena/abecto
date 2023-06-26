package de.uni_jena.cs.fusion.abecto.processor;

import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComparisonProcessorTest {

    @Test
    public void getResourceKeys() {
        Model primaryDataModel = ModelFactory.createDefaultModel();
        Property property = ResourceFactory.createProperty("http://example.org/property");
        Resource aspectIri = ResourceFactory.createResource("http://example.org/aspect");
        Resource dataset = ResourceFactory.createResource("http://example.org/dataset");
        Resource resource1 = ResourceFactory.createResource("http://example.org/1");
        Resource resource2 = ResourceFactory.createResource("http://example.org/2");
        Resource resource3 = ResourceFactory.createResource("http://example.org/3");
        Resource resource4 = ResourceFactory.createResource("http://example.org/4");
        primaryDataModel.addLiteral(resource1, property, 1);
        primaryDataModel.addLiteral(resource2, property, 2);
        primaryDataModel.addLiteral(resource3, property, 3);
        primaryDataModel.addLiteral(resource4, property, 4);
        Aspect aspect = new Aspect(aspectIri, "key");
        Query pattern = QueryFactory.create("SELECT ?key ?value WHERE {?key <" + property.getURI() + "> ?value .}");
        aspect.setPattern(dataset, pattern);

        Set<Resource> resources = new DummyComparisonProcessor().getResourceKeys(aspect, dataset, primaryDataModel).collect(Collectors.toSet()	);

        assertEquals(4, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
        assertTrue(resources.contains(resource3));
        assertTrue(resources.contains(resource4));
    }

    private static class DummyComparisonProcessor extends ComparisonProcessor<DummyComparisonProcessor> {
        @Override
        public void run() {}
    }
}
