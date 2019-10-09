package de.uni_jena.cs.fusion.abecto.sparql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.model.Models;
import de.uni_jena.cs.fusion.abecto.sparq.AbstractSparqlEntity;
import de.uni_jena.cs.fusion.abecto.sparq.Namespace;
import de.uni_jena.cs.fusion.abecto.sparq.PropertyPattern;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;

public class SparqlEntityManagerTest {

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithCollection extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:friend")
		public Collection<String> friends = new ArrayList<>();
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	@Namespace(prefix = "ex2", namespace = "http://example.com/")
	public static class EntityWithMultipleNamespaces extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex2:age")
		public Integer age;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithOptional extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:partner")
		public Optional<String> partner;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithoutAnnotation extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		public Resource boss;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithPropertyPath extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:boss/ex:boss")
		public Resource bigBoss;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResource extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:boss")
		public Resource boss;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResourceCollection extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:friend")
		public Collection<Resource> friends = new ArrayList<>();
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResourceOptional extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:partner")
		public Optional<Resource> partner;
	}

	@Namespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithUninitializedCollection extends AbstractSparqlEntity {
		@PropertyPattern("ex:name")
		public String name;
		@PropertyPattern("ex:friend")
		public Collection<String> friends;
	}

	private OntModel model;

	@Test
	public void insertAndSelectCollection()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithCollection notEmpty = new EntityWithCollection();
		notEmpty.name = "notEmpty";
		notEmpty.friends = Arrays.asList("Bob", "Charlie");
		EntityWithCollection empty = new EntityWithCollection();
		empty.name = "empty";
		empty.friends = Collections.emptySet();
		EntityWithCollection undefined = new EntityWithCollection();
		undefined.name = "undefined";
		undefined.friends = null;

		Assertions.assertThrows(NullPointerException.class, () -> {
			SparqlEntityManager.insert(Collections.singleton(undefined), model);
		});

		SparqlEntityManager.insert(Arrays.asList(notEmpty, empty), model);

		EntityWithCollection pattern = new EntityWithCollection();
		Set<EntityWithCollection> select;

		pattern.name = "notEmpty";
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(notEmpty.name, select.iterator().next().name);
		Assertions.assertEquals(new HashSet<>(notEmpty.friends), new HashSet<>(select.iterator().next().friends));

		pattern.name = "empty";
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(empty.name, select.iterator().next().name);
		Assertions.assertEquals(new HashSet<>(empty.friends), new HashSet<>(select.iterator().next().friends));
	}

	@Test
	public void insertAndSelectId() throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithOptional alice = new EntityWithOptional();
		alice.id(ResourceFactory.createResource("http://example.org/Alice"));
		alice.name = "Alice";
		alice.partner = Optional.empty();

		SparqlEntityManager.insert(Collections.singleton(alice), model);

		Set<EntityWithOptional> select = SparqlEntityManager.select(new EntityWithOptional(), model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(alice.id(), select.iterator().next().id());
		Assertions.assertEquals(alice.name, select.iterator().next().name);
	}

	@Test
	public void insertAndSelectMissingFieldAnnotation() {
		EntityWithResource with = new EntityWithResource();
		with.name = "Alice";
		with.boss = ResourceFactory.createResource("http://example.org/boss");
		EntityWithoutAnnotation without = new EntityWithoutAnnotation();
		without.name = with.name;
		without.boss = with.boss;

		Assertions.assertThrows(NullPointerException.class, () -> {
			SparqlEntityManager.insert(Collections.singleton(without), model);
		});

		SparqlEntityManager.insert(Collections.singleton(with), model);
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			SparqlEntityManager.select(new EntityWithoutAnnotation(), model);
		});
	}

	@Test
	public void insertAndSelectMutlipleNamespaces()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithMultipleNamespaces a = new EntityWithMultipleNamespaces();
		a.name = "Alice";
		a.age = 25;

		SparqlEntityManager.insert(Collections.singleton(a), model);

		Set<EntityWithMultipleNamespaces> select = SparqlEntityManager.select(new EntityWithMultipleNamespaces(),
				model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(a.age, select.iterator().next().age);
		Assertions.assertEquals(a.name, select.iterator().next().name);
	}

	@Test
	public void insertAndSelectOptional()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithOptional present = new EntityWithOptional();
		present.name = "Alice";
		present.partner = Optional.of("Bob");
		EntityWithOptional unknown = new EntityWithOptional();
		unknown.name = "Bob";
		unknown.partner = null;
		EntityWithOptional empty = new EntityWithOptional();
		empty.name = "Charlie";
		empty.partner = Optional.empty();

		Assertions.assertThrows(NullPointerException.class, () -> {
			SparqlEntityManager.insert(Collections.singleton(unknown), model);
		});

		SparqlEntityManager.insert(Arrays.asList(present, empty), model);

		EntityWithOptional pattern = new EntityWithOptional();
		Set<EntityWithOptional> select;

		pattern.name = "Alice";
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(present.name, select.iterator().next().name);
		Assertions.assertEquals(present.partner, select.iterator().next().partner);

		pattern.name = "Bob";
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(0, select.size());

		pattern.name = "Charlie";
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(empty.name, select.iterator().next().name);
		Assertions.assertEquals(empty.partner, select.iterator().next().partner);
	}

	@Test
	public void insertAndSelectResource()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResource a = new EntityWithResource();
		a.name = "Alice";
		a.boss = ResourceFactory.createResource("http://example.org/boss");

		SparqlEntityManager.insert(Collections.singleton(a), model);

		Set<EntityWithResource> select = SparqlEntityManager.select(new EntityWithResource(), model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(a.name, select.iterator().next().name);
		Assertions.assertEquals(a.boss, select.iterator().next().boss);
	}

	@Test
	public void insertAndSelectResourceCollection()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResourceCollection alice = new EntityWithResourceCollection();
		alice.name = "Alice";
		alice.friends = Arrays.asList(ResourceFactory.createResource("http://example.org/Bob"),
				ResourceFactory.createResource("http://example.org/Charlie"));

		SparqlEntityManager.insert(Arrays.asList(alice), model);

		Set<EntityWithResourceCollection> select = SparqlEntityManager.select(new EntityWithResourceCollection(),
				model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(alice.name, select.iterator().next().name);
		Assertions.assertEquals(new HashSet<>(alice.friends), new HashSet<>(select.iterator().next().friends));
	}

	@Test
	public void insertAndSelectResourceOptional()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResourceOptional alice = new EntityWithResourceOptional();
		alice.name = "Alice";
		alice.partner = Optional.of(ResourceFactory.createResource("http://example.org/bob"));

		SparqlEntityManager.insert(Collections.singleton(alice), model);

		Set<EntityWithResourceOptional> select = SparqlEntityManager.select(new EntityWithResourceOptional(), model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(alice.name, select.iterator().next().name);
		Assertions.assertEquals(alice.partner, select.iterator().next().partner);
	}

	@Test
	public void insertAndSelectUninitializedCollection()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithUninitializedCollection a = new EntityWithUninitializedCollection();
		a.name = "Alice";
		a.friends = Arrays.asList("Bob", "Charlie");

		SparqlEntityManager.insert(Collections.singleton(a), model);

		Assertions.assertThrows(NullPointerException.class, () -> {
			SparqlEntityManager.select(new EntityWithUninitializedCollection(), model);
		});
	}

	@Test
	public void insertPropertyPath() {
		EntityWithPropertyPath a = new EntityWithPropertyPath();
		a.name = "Alice";
		a.bigBoss = ResourceFactory.createResource("http://example.org/charlie");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SparqlEntityManager.insert(Collections.singleton(a), model);
		});
	}

	@BeforeEach
	public void prepareModel() {
		this.model = Models.getEmptyOntModel();
	}

	@Test
	public void selectPropertyPath() throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResource alice = new EntityWithResource();
		alice.id(ResourceFactory.createResource("http://example.org/alice"));
		alice.name = "Alice";
		alice.boss = ResourceFactory.createResource("http://example.org/alice");
		EntityWithResource bob = new EntityWithResource();
		bob.id(ResourceFactory.createResource("http://example.org/bob"));
		bob.name = "Bob";
		bob.boss = alice.id();
		EntityWithResource charlie = new EntityWithResource();
		charlie.id(ResourceFactory.createResource("http://example.org/charlie"));
		charlie.name = "Charlie";
		charlie.boss = bob.id();

		SparqlEntityManager.insert(Arrays.asList(alice, bob, charlie), model);

		EntityWithPropertyPath pattern = new EntityWithPropertyPath();
		pattern.name = "Charlie";
		Set<EntityWithPropertyPath> select = SparqlEntityManager.select(pattern, model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(charlie.name, select.iterator().next().name);
		Assertions.assertEquals(alice.id(), select.iterator().next().bigBoss);
	}
}
