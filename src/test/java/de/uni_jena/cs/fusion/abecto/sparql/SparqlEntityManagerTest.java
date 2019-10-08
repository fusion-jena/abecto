package de.uni_jena.cs.fusion.abecto.sparql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.AbstractSparqlEntity;
import de.uni_jena.cs.fusion.abecto.sparq.Namespace;
import de.uni_jena.cs.fusion.abecto.sparq.PropertyPattern;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class SparqlEntityManagerTest {

	private OntModel model;

	@BeforeEach
	public void prepareModel() {
		this.model = Models.getEmptyOntModel();
	}

	@Test
	public void insertAndSelectMutlipleNamespaces()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithMultipleNamespace a = new EntityWithMultipleNamespace();
		a.name = "Alice";
		a.age = 25;

		SparqlEntityManager.insert(Collections.singleton(a), model);

		Set<EntityWithMultipleNamespace> select = SparqlEntityManager.select(new EntityWithMultipleNamespace(), model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(a.age, select.iterator().next().age);
		Assertions.assertEquals(a.name, select.iterator().next().name);
	}

	@Test
	public void insertPropertyPath() {
		EntityWithPropertyPath a = new EntityWithPropertyPath();
		a.name = "Alice";
		a.fathersName = "Bob";

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SparqlEntityManager.insert(Collections.singleton(a), model);
		});
	}

	@Test
	public void selectPropertyPath() {
	}

	@Test
	public void selectOptional() {
	}

	@Test
	public void insertOptional() {
	}

	@Test
	public void insertAndSelectCollection()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithCollection a = new EntityWithCollection();
		a.name = "Alice";
		a.friends = Arrays.asList("Bob", "Charlie");

		SparqlEntityManager.insert(Collections.singleton(a), model);

		Set<EntityWithCollection> select = SparqlEntityManager.select(new EntityWithCollection(), model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(a.name, select.iterator().next().name);
		Assertions.assertEquals(2, select.iterator().next().friends.size());
		Assertions.assertTrue(select.iterator().next().friends.containsAll(a.friends));
		Assertions.assertEquals(a.friends, select.iterator().next().friends);
	}

	@Test
	public void selectResource() {
	}

	@Test
	public void insertResource() {
	}

	@Test
	public void selectString() {
	}

	@Test
	public void insertString() {
	}

	@Test
	public void selectMissingFieldAnnotation() {
	}

	@Test
	public void insertMissingFieldAnnotation() {
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithPropertyPath extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:father/ex:age")
		public String fathersName;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithCollection extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:friend")
		public Collection<String> friends;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	@Namespace(prefix = "ex2", namespace = "http://example.com/")
	public static class EntityWithMultipleNamespace extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex2:age")
		public Integer age;
	}
}
