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
import de.uni_jena.cs.fusion.abecto.sparq.SparqlEntityManager;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlNamespace;
import de.uni_jena.cs.fusion.abecto.sparq.SparqlPattern;

public class SparqlEntityManagerTest {

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithCollection {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:friend")
		public Collection<String> friends = new ArrayList<>();
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	@SparqlNamespace(prefix = "ex2", namespace = "http://example.com/")
	public static class EntityWithMultipleNamespaces {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex2:age")
		public Integer age;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithOptional {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:partner")
		public Optional<String> partner;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithoutAnnotation {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		public Resource boss;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithPropertyPath {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:boss/ex:boss")
		public Resource bigBoss;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResource {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:boss")
		public Resource boss;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResourceCollection {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:friend")
		public Collection<Resource> friends = new ArrayList<>();
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithResourceOptional {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:partner")
		public Optional<Resource> partner;
	}

	@SparqlNamespace(prefix = "ex", namespace = "http://example.org/")
	public static class EntityWithUninitializedCollection {
		public Resource id;
		@SparqlPattern(subject = "id", predicate = "ex:name")
		public String name;
		@SparqlPattern(subject = "id", predicate = "ex:friend")
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

		pattern.name = null;
		pattern.friends = Arrays.asList("Bob", "Charlie");
		select = SparqlEntityManager.select(pattern, model);
		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(notEmpty.name, select.iterator().next().name);
		Assertions.assertEquals(new HashSet<>(notEmpty.friends), new HashSet<>(select.iterator().next().friends));

	}

	@Test
	public void insertAndSelectId() throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithOptional alice = new EntityWithOptional();
		alice.id = ResourceFactory.createResource("http://example.org/Alice");
		alice.name = "Alice";
		alice.partner = Optional.empty();

		SparqlEntityManager.insert(Collections.singleton(alice), model);

		Set<EntityWithOptional> select = SparqlEntityManager.select(new EntityWithOptional(), model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(alice.id, select.iterator().next().id);
		Assertions.assertEquals(alice.name, select.iterator().next().name);
	}

	@Test
	public void insertAndSelectMissingFieldAnnotation()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResource with = new EntityWithResource();
		with.name = "Alice";
		with.boss = ResourceFactory.createResource("http://example.org/boss");
		EntityWithoutAnnotation without = new EntityWithoutAnnotation();
		without.name = with.name;
		without.boss = with.boss;

		SparqlEntityManager.insert(Collections.singleton(without), model);

		SparqlEntityManager.insert(Collections.singleton(with), model);

		Assertions.assertThrows(IllegalStateException.class, () -> {
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
	public void selectById() throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithOptional alice1 = new EntityWithOptional();
		alice1.id = ResourceFactory.createResource("http://example.org/Alice1");
		alice1.name = "Alice";
		alice1.partner = Optional.empty();
		EntityWithOptional alice2 = new EntityWithOptional();
		alice2.id = ResourceFactory.createResource("http://example.org/Alice2");
		alice2.name = "Alice";
		alice2.partner = Optional.empty();

		SparqlEntityManager.insert(Arrays.asList(alice1, alice2), model);

		alice1.name = null;
		alice1.partner = null;
		Set<EntityWithOptional> select = SparqlEntityManager.select(alice1, model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(alice1.id, select.iterator().next().id);
	}

	@Test
	public void selectPropertyPath() throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResource alice = new EntityWithResource();
		alice.id = ResourceFactory.createResource("http://example.org/alice");
		alice.name = "Alice";
		alice.boss = ResourceFactory.createResource("http://example.org/alice");
		EntityWithResource bob = new EntityWithResource();
		bob.id = ResourceFactory.createResource("http://example.org/bob");
		bob.name = "Bob";
		bob.boss = alice.id;
		EntityWithResource charlie = new EntityWithResource();
		charlie.id = ResourceFactory.createResource("http://example.org/charlie");
		charlie.name = "Charlie";
		charlie.boss = bob.id;

		SparqlEntityManager.insert(Arrays.asList(alice, bob, charlie), model);

		EntityWithPropertyPath pattern = new EntityWithPropertyPath();
		pattern.name = "Charlie";
		Set<EntityWithPropertyPath> select = SparqlEntityManager.select(pattern, model);

		Assertions.assertEquals(1, select.size());
		Assertions.assertEquals(charlie.name, select.iterator().next().name);
		Assertions.assertEquals(alice.id, select.iterator().next().bigBoss);
	}

	@Test
	public void selectResourceCollectionByMultipleEntities()
			throws IllegalStateException, NullPointerException, ReflectiveOperationException {
		EntityWithResourceCollection alice = new EntityWithResourceCollection();
		alice.name = "Alice";
		alice.friends = Arrays.asList(ResourceFactory.createResource("http://example.org/Bob"),
				ResourceFactory.createResource("http://example.org/Charlie"));
		EntityWithResourceCollection bob = new EntityWithResourceCollection();
		bob.name = "Bob";
		bob.friends = Arrays.asList(ResourceFactory.createResource("http://example.org/Alice"),
				ResourceFactory.createResource("http://example.org/Charlie"));

		SparqlEntityManager.insert(Arrays.asList(alice, bob), model);

		EntityWithResourceCollection aliceFilter = new EntityWithResourceCollection();
		aliceFilter.name = "Alice";
		EntityWithResourceCollection bobFilter = new EntityWithResourceCollection();
		bobFilter.name = "Bob";

		Set<EntityWithResourceCollection> select = SparqlEntityManager.select(Arrays.asList(aliceFilter, bobFilter),
				model);

		Assertions.assertEquals(2, select.size());
		Assertions.assertEquals(new HashSet<>(alice.friends),
				new HashSet<>(select.stream().filter((x) -> x.name.equals("Alice")).findAny().get().friends));
		Assertions.assertEquals(new HashSet<>(bob.friends),
				new HashSet<>(select.stream().filter((x) -> x.name.equals("Bob")).findAny().get().friends));
	}
}
