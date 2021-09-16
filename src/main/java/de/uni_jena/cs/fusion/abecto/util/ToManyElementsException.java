/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
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
package de.uni_jena.cs.fusion.abecto.util;

public class ToManyElementsException extends RuntimeException {
	private static final long serialVersionUID = -3392659781990973252L;

	/**
	 * Constructs a {@code ToManyElementsException} with {@code null} as its error
	 * message string.
	 */
	public ToManyElementsException() {
		super();
	}

	/**
	 * Constructs a {@code ToManyElementsException}, saving a reference to the error
	 * message string {@code s} for later retrieval by the {@code getMessage}
	 * method.
	 *
	 * @param s the detail message.
	 */
	public ToManyElementsException(String s) {
		super(s);
	}
}