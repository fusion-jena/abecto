package de.uni_jena.cs.fusion.abecto.processing;

import de.uni_jena.cs.fusion.abecto.processor.api.Processor;

/**
 * Thrown when the instantiation of a {@link Processor} failed for a variety of
 * reasons and wrapping the initial reason.
 */
public class ProcessorInstantiationException extends Exception {
	private static final long serialVersionUID = 4510416723049958258L;

	public ProcessorInstantiationException(Throwable cause) {
		super(cause);
	}

}
