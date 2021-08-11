package de.uni_jena.cs.fusion.abecto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.util.ToManyElementsException;

public class ParametersTest {
	final static Temporal defaultValue = ZonedDateTime.now();

	@Test
	public void setParameters() throws Exception {

		List<Temporal> expectedOne = Arrays.asList(LocalDate.now());

		List<Temporal> expectedMultiple = Arrays.asList(LocalDate.now(), LocalTime.now(), LocalDateTime.now());

		// collection: no value
		{
			CollectionProcessor processor = new CollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// collection: one value
		{
			CollectionProcessor processor = new CollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}
		// collection: multiple value
		{
			CollectionProcessor processor = new CollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}

		// default collection: no value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.contains(defaultValue));
			assertEquals(1, processor.parameter.size());
		}
		// default collection: one value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}
		// default collection: multiple value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}

		// uninitialized collection: no value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// uninitialized collection: one value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}
		// uninitialized collection: multiple value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(l -> new Temporal[l]),
					processor.parameter.toArray(l -> new Temporal[l]));
		}

		// optional: no value
		{
			OptionalProcessor processor = new OptionalProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// optional: one value
		{
			OptionalProcessor processor = new OptionalProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter.get());
		}
		// optional: multiple value
		{
			OptionalProcessor processor = new OptionalProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}

		// required: no value
		{
			RequiredProcessor processor = new RequiredProcessor();
			assertThrows(NoSuchElementException.class,
					() -> Parameters.setParameters(processor, Collections.emptyMap()));
		}
		// required: one value
		{
			RequiredProcessor processor = new RequiredProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter);
		}
		// required: multiple value
		{
			RequiredProcessor processor = new RequiredProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}

		// default: no value
		{
			DefaultProcessor processor = new DefaultProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertEquals(defaultValue, processor.parameter);
		}
		// default: one value
		{
			DefaultProcessor processor = new DefaultProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter);
		}
		// default: multiple value
		{
			DefaultProcessor processor = new DefaultProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}
	}

	private static class CollectionProcessor extends Processor {
		@Parameter
		public Collection<Temporal> parameter = new ArrayList<>();

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class UninitializedCollectionProcessor extends Processor {
		@Parameter
		public Collection<Temporal> parameter = new ArrayList<>();

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class DefaultCollectionProcessor extends Processor {
		@Parameter
		public Collection<Temporal> parameter = new ArrayList<>(Collections.singletonList(defaultValue));

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class OptionalProcessor extends Processor {
		@Parameter
		public Optional<Temporal> parameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class RequiredProcessor extends Processor {
		@Parameter
		public Temporal parameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class DefaultProcessor extends Processor {
		@Parameter
		public Temporal parameter = defaultValue;

		@Override
		public void run() {
			// do nothing
		}
	}
}
