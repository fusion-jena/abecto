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