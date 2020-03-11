package de.uni_jena.cs.fusion.abecto.processor;

import java.util.UUID;

import de.uni_jena.cs.fusion.abecto.parameter_model.ParameterModel;

public interface SourceProcessor<P extends ParameterModel> extends Processor<P> {
	
	/**
	 * @param uuid {@link UUID} of the knowledge base
	 */
	public void setOntology(UUID uuid);

}
