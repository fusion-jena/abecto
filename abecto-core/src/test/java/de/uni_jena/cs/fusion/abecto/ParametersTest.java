/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

		// collection: null value
		{
			EmptyCollectionProcessor processor = new EmptyCollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// collection: no value
		{
			EmptyCollectionProcessor processor = new EmptyCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", Collections.emptyList()));
			assertTrue(processor.parameter.isEmpty());
		}
		// collection: one value
		{
			EmptyCollectionProcessor processor = new EmptyCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}
		// collection: multiple values
		{
			EmptyCollectionProcessor processor = new EmptyCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}

		// default collection: null value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.contains(defaultValue));
		}
		// default collection: no value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", Collections.emptyList()));
			assertTrue(processor.parameter.isEmpty());
		}
		// default collection: one value
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}
		// default collection: multiple values
		{
			DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}

		// uninitialized collection: null value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// uninitialized collection: no value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", Collections.emptyList()));
			assertTrue(processor.parameter.isEmpty());
		}
		// uninitialized collection: one value
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertArrayEquals(expectedOne.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}
		// uninitialized collection: multiple values
		{
			UninitializedCollectionProcessor processor = new UninitializedCollectionProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple));
			assertArrayEquals(expectedMultiple.toArray(Temporal[]::new),
					processor.parameter.toArray(Temporal[]::new));
		}

		// optional: null value
		{
			OptionalProcessor processor = new OptionalProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertTrue(processor.parameter.isEmpty());
		}
		// optional: no value
		{
			OptionalProcessor processor = new OptionalProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", Collections.emptyList()));
			assertTrue(processor.parameter.isEmpty());
		}
		// optional: one value
		{
			OptionalProcessor processor = new OptionalProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter.get());
		}
		// optional: multiple values
		{
			OptionalProcessor processor = new OptionalProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}

		// required: null value
		{
			RequiredProcessor processor = new RequiredProcessor();
			assertThrows(NoSuchElementException.class,
					() -> Parameters.setParameters(processor, Collections.emptyMap()));
		}
		// required: no value
		{
			RequiredProcessor processor = new RequiredProcessor();
			assertThrows(NoSuchElementException.class, () -> Parameters.setParameters(processor,
					Collections.singletonMap("parameter", Collections.emptyList())));
		}
		// required: one value
		{
			RequiredProcessor processor = new RequiredProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter);
		}
		// required: multiple values
		{
			RequiredProcessor processor = new RequiredProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}

		// default: null value
		{
			DefaultProcessor processor = new DefaultProcessor();
			Parameters.setParameters(processor, Collections.emptyMap());
			assertEquals(defaultValue, processor.parameter);
		}
		// default: no value
		{
			DefaultProcessor processor = new DefaultProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", Collections.emptyList()));
			assertEquals(defaultValue, processor.parameter);
		}
		// default: one value
		{
			DefaultProcessor processor = new DefaultProcessor();
			Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedOne));
			assertEquals(expectedOne.get(0), processor.parameter);
		}
		// default: multiple values
		{
			DefaultProcessor processor = new DefaultProcessor();
			assertThrows(ToManyElementsException.class,
					() -> Parameters.setParameters(processor, Collections.singletonMap("parameter", expectedMultiple)));
		}
	}

	private static class EmptyCollectionProcessor extends Processor<EmptyCollectionProcessor> {
		@Parameter
		public Collection<Temporal> parameter = new ArrayList<>();
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class UninitializedCollectionProcessor extends Processor<UninitializedCollectionProcessor> {
		@Parameter
		public Collection<Temporal> parameter;
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class DefaultCollectionProcessor extends Processor<DefaultCollectionProcessor> {
		@Parameter
		public Collection<Temporal> parameter = new ArrayList<>(Collections.singletonList(defaultValue));
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class OptionalProcessor extends Processor<OptionalProcessor> {
		@Parameter
		public Optional<Temporal> parameter;
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class RequiredProcessor extends Processor<RequiredProcessor> {
		@Parameter
		public Temporal parameter;
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}

	private static class DefaultProcessor extends Processor<DefaultProcessor> {
		@Parameter
		public Temporal parameter = defaultValue;
		@SuppressWarnings("unused")
		public Temporal notAParameter;

		@Override
		public void run() {
			// do nothing
		}
	}
}
