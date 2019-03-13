package de.uni_jena.cs.fusion.abecto;

public class UnexpectedStateException extends IllegalStateException {
	/**
	 * Constructs an UnexpectedStateException with a detail message asking to report
	 * this issue.
	 */
	public UnexpectedStateException() {
		super("Unexpected state. Please report this issue."); // TODO add issue tracker link
	}

	private static final long serialVersionUID = 2109554168182471298L;
}
