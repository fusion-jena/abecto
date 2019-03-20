package de.uni_jena.cs.fusion.abecto.processing;

public class ProcessingException extends Exception {
	private static final long serialVersionUID = 4507928925041672680L;

	public ProcessingException() {
		super();
	}

	public ProcessingException(String message) {
		super(message);
	}

	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessingException(Throwable cause) {
		super(cause);
	}
}
