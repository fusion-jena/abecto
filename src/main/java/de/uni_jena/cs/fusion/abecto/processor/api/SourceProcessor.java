package de.uni_jena.cs.fusion.abecto.processor.api;

import java.util.UUID;

public interface SourceProcessor<P extends ParameterModel> extends Processor<P> {
	
	/**
	 * @param uuid {@link UUID} of the knowledge base
	 */
	public void setKnowledgBase(UUID uuid);

}
