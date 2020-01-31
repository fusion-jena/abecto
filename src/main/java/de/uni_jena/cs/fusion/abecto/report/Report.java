package de.uni_jena.cs.fusion.abecto.report;

import de.uni_jena.cs.fusion.abecto.processor.Processor;

public interface Report {

	Object of(Processor<?> processor) throws Exception;

}
